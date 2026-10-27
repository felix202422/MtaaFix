# Phase 19 (B-16.3): point-in-time-recovery drill on a SCRATCH PostgreSQL 18
# cluster (never touches the service cluster on 5432 or the mtaafix database).
#
# What it proves, end to end:
#   1. WAL archiving on (archive_command -> work archive dir)
#   2. base backup via pg_basebackup -X stream
#   3. three markers: A before backup, B after backup, C after the target time
#   4. recovery of a copied base backup with restore_command +
#      recovery_target_time (between B and C) and recovery_target_action=promote
#   5. the recovered database contains A and B but NOT C
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File scripts\pitr-drill.ps1
#   ... -KeepWorkDir    # keep pgdata/archive/basebackup for inspection
param(
    [int]$Port = 55432,
    [int]$RecoveryPort = 55433,
    [string]$WorkDir = (Join-Path $env:TEMP 'mtaafix_pitr_drill'),
    [string]$EvidenceFile = (Join-Path $PSScriptRoot 'pitr-drill-evidence.txt'),
    [switch]$KeepWorkDir
)

$ErrorActionPreference = 'Stop'
# Hardening (2nd run): convert silent hangs into fast, diagnosable failures.
$env:PGCONNECT_TIMEOUT = '15'   # psql/pg_basebackup connect timeout (s)
foreach ($tool in @('initdb', 'pg_ctl', 'psql', 'pg_basebackup')) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "$tool not found on PATH - install PostgreSQL 18 server tools."
    }
}

$ev = New-Object System.Collections.Generic.List[string]
$stepLog = Join-Path $env:TEMP 'mtaafix_pitr_drill_steps.log'
function Note([string]$m) {
    Write-Host "[drill] $m"
    $script:ev.Add($m)
    # Mirror every step to a file so a hang can be located even if the
    # console output is lost (first run stalled with no visible trace).
    try { Add-Content -Path $stepLog -Value "[$((Get-Date).ToString('HH:mm:ss'))] $m" } catch { }
}

function Invoke-Psql([int]$p, [string]$sql, [string]$db = 'postgres') {
    & psql -h localhost -p $p -U postgres -d $db -tAc $sql
    if ($LASTEXITCODE -ne 0) { throw "psql failed (port ${p}): $sql" }
}

function Wait-Ready([int]$p, [int]$seconds) {
    for ($i = 0; $i -lt $seconds; $i++) {
        # Output captured via $null (not a native *> redirect); bounded by
        # PGCONNECT_TIMEOUT so a wedged postmaster fails the poll fast.
        $null = & psql -h localhost -p $p -U postgres -d postgres -tAc 'SELECT 1'
        if ($LASTEXITCODE -eq 0) { Note "port $p ready (after $($i + 1) s)"; return }
        Start-Sleep -Seconds 1
    }
    throw "PostgreSQL on port $p not ready after $seconds s"
}

function Wait-Promoted([int]$p, [int]$seconds) {
    # Mere connectivity is NOT proof that recovery finished: hot standby
    # accepts read-only connections WHILE replay is still in flight
    # (pg_is_in_recovery() = t). The first full drill's verify query raced
    # replay and read the pre-marker-B state, failing an otherwise-correct
    # PITR. Only promotion (recovery complete to the target) is verifiable.
    for ($i = 0; $i -lt $seconds; $i++) {
        $r = & psql -h localhost -p $p -U postgres -d postgres -tAc 'SELECT pg_is_in_recovery()'
        if ($LASTEXITCODE -eq 0 -and "$r".Trim() -eq 'f') {
            Note "recovery promoted (pg_is_in_recovery()=f after $($i + 1) s)"
            return
        }
        Start-Sleep -Seconds 1
    }
    throw "cluster on port $p did not promote within $seconds s"
}

function Wait-Archive([string]$dir, [int]$min, [int]$seconds) {
    $statusDir = Join-Path $WorkDir 'pgdata\pg_wal\archive_status'
    for ($i = 0; $i -lt $seconds; $i++) {
        # -Force so hidden/system files are counted too. A plain count is not
        # enough: PREVIOUS segments can satisfy it while the marker's segment
        # is still in flight — also require that no archiving is pending
        # (every queued segment has a .ready file until it has landed).
        $ready = @(Get-ChildItem -Path $statusDir -Filter '*.ready' -Force -File -ErrorAction SilentlyContinue).Count
        $n = @(Get-ChildItem -Path $dir -Force -File -ErrorAction SilentlyContinue).Count
        if ($ready -eq 0 -and $n -ge $min) { return $n }
        Start-Sleep -Milliseconds 500
    }
    # Dump everything needed to explain the miss: the dir listing, PG's view
    # (.ready/.done), the archive wrapper's own log, and the server log tail.
    # (Run 1 of this drill saw 4 successful copies with no visible files and
    # gave no way to tell where they went.)
    $files = (@(Get-ChildItem -Path $dir -Force -File -ErrorAction SilentlyContinue |
            ForEach-Object Name) -join ', ')
    $statusDir = Join-Path $WorkDir 'pgdata\pg_wal\archive_status'
    $status = (@(Get-ChildItem -Path $statusDir -Force -File -ErrorAction SilentlyContinue |
            ForEach-Object Name) -join ', ')
    $wLog = "(wrapper log absent)"
    if ($script:wrapperLog -and (Test-Path $script:wrapperLog)) {
        $wLog = (Get-Content $script:wrapperLog -Tail 10) -join ' | '
    }
    $pLog = "(no server log)"
    $svrLog = Join-Path $WorkDir 'pg1.log'
    if (Test-Path $svrLog) { $pLog = (Get-Content $svrLog -Tail 8) -join ' | ' }
    throw ("archive dir holds < $min file(s) after $seconds s`n" +
    "  dir[$dir]: [$files]`n  archive_status: [$status]`n" +
    "  archive wrapper: $wLog`n  server log: $pLog")
}

function Stop-Cluster([int]$p, [string]$d, [string]$logName) {
    # -t bounds the stop wait; no native-output redirect (hardening).
    & pg_ctl -D $d -l (Join-Path $WorkDir $logName) -m fast stop -t 60
    if ($LASTEXITCODE -ne 0) { Write-Host "[warn] stop($logName) exit $LASTEXITCODE" }
}

# --- 0. Layout --------------------------------------------------------------
# Stop any cluster left running by a previous failed run (its files would
# otherwise be locked and Remove-Item would fail mid-cleanup).
foreach ($leftover in @('pgdata', 'recovered')) {
    $pidFile = Join-Path $WorkDir (Join-Path $leftover 'postmaster.pid')
    if (Test-Path $pidFile) {
        Note "cleanup: stopping leftover $leftover cluster from a previous run"
        & pg_ctl -D (Join-Path $WorkDir $leftover) -m immediate stop -t 30
    }
}
if (Test-Path $stepLog) { Remove-Item -Force $stepLog }
if (Test-Path $WorkDir) { Remove-Item -Recurse -Force $WorkDir }
$pgdata = Join-Path $WorkDir 'pgdata'
$archive = Join-Path $WorkDir 'archive'
$base = Join-Path $WorkDir 'basebackup'
$recovered = Join-Path $WorkDir 'recovered'
New-Item -ItemType Directory -Force $WorkDir, $archive | Out-Null

Note "drill start (UTC $((Get-Date).ToUniversalTime().ToString('o')))"
Note "psql: $(& psql --version)"
Note "workdir: $WorkDir (scratch cluster on ports $Port/$RecoveryPort)"

# --- 1. initdb --------------------------------------------------------------
& initdb -D $pgdata -U postgres --auth-local=trust --auth-host=trust --no-locale -E UTF8
if ($LASTEXITCODE -ne 0) { throw "initdb failed ($LASTEXITCODE)" }
Note 'initdb: scratch cluster created (auth=trust, UTF8)'

# --- 2. archiving configuration --------------------------------------------
$conf = Join-Path $pgdata 'postgresql.conf'
# Archive through a logging wrapper instead of a bare `copy`: every invocation
# records timestamp, cwd, resolved source, and the post-copy Test-Path, so a
# Wait-Archive timeout is diagnosable from archive-command.log alone. The
# wrapper also resolves PostgreSQL's RELATIVE %p (e.g. pg_wal\0000...) against
# the scratch pgdata explicitly, instead of trusting the child's cwd.
$wrapper = Join-Path $WorkDir 'archive-command.ps1'
$script:wrapperLog = Join-Path $WorkDir 'archive-command.log'
Set-Content -Path $wrapper -Value @'
param(
    [Parameter(Position = 0)][string]$Src,
    [Parameter(Position = 1)][string]$Name
)
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$archive = Join-Path $here 'archive'
$log = Join-Path $here 'archive-command.log'
if (-not [System.IO.Path]::IsPathRooted($Src)) {
    $Src = Join-Path $here (Join-Path 'pgdata' $Src)
}
$line = '[{0}] cwd=[{1}] src=[{2}] exists={3}' -f `
    (Get-Date -Format 'HH:mm:ss.fff'), (Get-Location).Path, $Src, (Test-Path -LiteralPath $Src)
try {
    Copy-Item -LiteralPath $Src -Destination (Join-Path $archive $Name) -Force -ErrorAction Stop
    $dst = Join-Path $archive $Name
    $line += (' dstExists={0} bytes={1}' -f `
        (Test-Path -LiteralPath $dst), (Get-Item -LiteralPath $dst -ErrorAction SilentlyContinue).Length)
    Add-Content -LiteralPath $log -Value $line
    exit 0
} catch {
    $line += (' COPY_FAILED={0}' -f $_.Exception.Message)
    Add-Content -LiteralPath $log -Value $line
    exit 1
}
'@
# NOTE: hand-written conf values are parsed as GUC strings where BACKSLASH IS
# AN ESCAPE CHARACTER — 'C:\Users\...' would load as 'C:Users...' (root-caused
# the first drill: copy silently wrote a mangled filename into pgdata instead
# of the archive dir). So the wrapper path below uses FORWARD slashes, which
# GUC parsing leaves untouched and PowerShell accepts.
$wrapperFc = $wrapper -replace '\\', '/'
$archiveCmd = "archive_command = 'powershell -NoProfile -ExecutionPolicy Bypass -File `"$wrapperFc`" `"%p`" `"%f`"'"
Add-Content -Path $conf -Value @"

# --- Phase 19 PITR drill settings ---
port = $Port
wal_level = replica
archive_mode = on
$archiveCmd
"@
Note "config: port=$Port wal_level=replica archive_mode=on archive_command=wrapper ($wrapperFc)"

# --- 3. start + marker A ----------------------------------------------------
& pg_ctl -D $pgdata -l (Join-Path $WorkDir 'pg1.log') -o "-p $Port" -t 60 start
if ($LASTEXITCODE -ne 0) { throw "pg_ctl start failed ($LASTEXITCODE)" }
Wait-Ready $Port 30
# Round-trip proof: SHOW returns the PARSED value — if any path mangling had
# survived GUC parsing, it would be visible right here in the evidence.
Note ("SHOW archive_command: " + (Invoke-Psql $Port 'SHOW archive_command').Trim())
Invoke-Psql $Port 'CREATE DATABASE mtaafix_pitr'
Invoke-Psql $Port 'CREATE TABLE drill (id serial PRIMARY KEY, phase text NOT NULL)' 'mtaafix_pitr'
Invoke-Psql $Port "INSERT INTO drill (phase) VALUES ('phase-a-before-base')" 'mtaafix_pitr'
Note 'marker A: phase-a-before-base inserted'

# --- 4. base backup ---------------------------------------------------------
# Bounded process launch (hardening): an unbounded console run stalled forever
# in run 1, and a Start-Job wrapper stalled on output plumbing in run 2;
# Start-Process never exposed ExitCode on this PowerShell (handle quirk), so we
# use System.Diagnostics.Process: WaitForExit(180 s) converts any hang into a
# timed throw, and ExitCode is readable. Output stays on the console (no pipe,
# no buffer-deadlock risk).
$bbArgs = "-h localhost -p $Port -U postgres -D `"$base`" -Fp -X stream -v"
$bbPsi = New-Object System.Diagnostics.ProcessStartInfo
$bbPsi.FileName = 'pg_basebackup'
$bbPsi.Arguments = $bbArgs
$bbPsi.UseShellExecute = $false          # inherit the console; no redirection
$bbPsi.CreateNoWindow = $false
$bbProc = [System.Diagnostics.Process]::Start($bbPsi)
if (-not $bbProc.WaitForExit(180000)) {
    try { $bbProc.Kill($true) } catch { try { $bbProc.Kill() } catch { } }
    throw 'pg_basebackup timed out after 180 s'
}
$bbProc.WaitForExit()   # finalize async handle so ExitCode is populated
$bbCode = $bbProc.ExitCode
if ($bbCode -ne 0) { throw "pg_basebackup failed (exit $bbCode)" }
$baseFiles = @(Get-ChildItem -Path $base -Recurse -File).Count
Note "base backup: pg_basebackup -X stream -> $baseFiles files"

# --- 5. marker B + target time + archive the segment ------------------------
Invoke-Psql $Port "INSERT INTO drill (phase) VALUES ('phase-b-after-base')" 'mtaafix_pitr'
$target = (Invoke-Psql $Port 'SELECT clock_timestamp()').Trim()
Invoke-Psql $Port 'CHECKPOINT'
$c0 = @(Get-ChildItem -Path $archive -Force -File -ErrorAction SilentlyContinue).Count
$switched = (Invoke-Psql $Port 'SELECT pg_switch_wal()').Trim()
# Note BEFORE waiting so a Wait-Archive timeout still leaves target time and
# switch LSN in the evidence log.
Note "marker B: phase-b-after-base inserted; target time = $target; switch_wal -> $switched; archive held $c0 file(s) pre-switch"
$n = Wait-Archive $archive ($c0 + 1) 60
Note "wal switch: archive grew $c0 -> $n (B's segment confirmed in archive)"

# --- 6. marker C + archive its segment too ----------------------------------
Invoke-Psql $Port "INSERT INTO drill (phase) VALUES ('phase-c-must-not-recover')" 'mtaafix_pitr'
$c1 = @(Get-ChildItem -Path $archive -Force -File -ErrorAction SilentlyContinue).Count
$switchedC = (Invoke-Psql $Port 'SELECT pg_switch_wal()').Trim()
Note "marker C: phase-c-must-not-recover inserted; switch_wal -> $switchedC; archive held $c1 file(s) pre-switch"
$n = Wait-Archive $archive ($c1 + 1) 60
Note "archive grew $c1 -> $n (C's segment confirmed in archive)"

# --- 7. stop source ---------------------------------------------------------
Stop-Cluster $Port $pgdata 'pg1.log'
Note 'source cluster stopped'

# --- 8. prepare recovery copy ----------------------------------------------
New-Item -ItemType Directory -Force $recovered | Out-Null
Copy-Item -Path (Join-Path $base '*') -Destination $recovered -Recurse -Force
$rConf = Join-Path $recovered 'postgresql.conf'
(Get-Content $rConf) -replace '^archive_mode', '# archive_mode' `
    -replace '^archive_command', '# archive_command' | Set-Content $rConf
# Restore through a logging wrapper, same rationale as archive_command, and
# again with a FORWARD-slash path (hand-written conf values treat backslash as
# an escape — the original copy-based restore_command was silently mangled).
# %p = destination path (may be absolute or pg_wal-relative), %f = archive name.
$restoreWrapper = Join-Path $recovered 'restore-command.ps1'
$restoreLog = Join-Path $WorkDir 'restore-command.log'
$script:restoreLog = $restoreLog
Set-Content -Path $restoreWrapper -Value @'
param(
    [Parameter(Position = 0)][string]$Dst,
    [Parameter(Position = 1)][string]$Name
)
$here = Split-Path -Parent $MyInvocation.MyCommand.Path
$work = Split-Path -Parent $here
$archive = Join-Path $work 'archive'
$log = Join-Path $work 'restore-command.log'
if (-not [System.IO.Path]::IsPathRooted($Dst)) { $Dst = Join-Path $here $Dst }
$src = Join-Path $archive $Name
$line = '[{0}] name=[{1}] src=[{2}] exists={3} dst=[{4}]' -f `
    (Get-Date -Format 'HH:mm:ss.fff'), $Name, $src, (Test-Path -LiteralPath $src), $Dst
if (-not (Test-Path -LiteralPath $src)) {
    Add-Content -LiteralPath $log -Value ($line + ' MISS')
    exit 1
}
try {
    Copy-Item -LiteralPath $src -Destination $Dst -Force -ErrorAction Stop
    Add-Content -LiteralPath $log -Value ($line + ' restored')
    exit 0
} catch {
    Add-Content -LiteralPath $log -Value ($line + ' RESTORE_FAILED=' + $_.Exception.Message)
    exit 1
}
'@
$restoreWrapperFc = $restoreWrapper -replace '\\', '/'
$restoreCmd = "restore_command = 'powershell -NoProfile -ExecutionPolicy Bypass -File `"$restoreWrapperFc`" `"%p`" `"%f`"'"
$auto = Join-Path $recovered 'postgresql.auto.conf'
Add-Content -Path $auto -Value @"
# --- Phase 19 PITR drill recovery settings ---
$restoreCmd
recovery_target_time = '$target'
recovery_target_action = 'promote'
"@
New-Item -ItemType File -Path (Join-Path $recovered 'recovery.signal') -Force | Out-Null
Note "recovery copy: restore_command -> archive, recovery_target_time = $target, action=promote"

# --- 9. recover -------------------------------------------------------------
& pg_ctl -D $recovered -l (Join-Path $WorkDir 'recovered.log') -o "-p $RecoveryPort" -t 60 start
if ($LASTEXITCODE -ne 0) { throw "recovery start failed ($LASTEXITCODE)" }
Wait-Ready $RecoveryPort 60
# Wait for PROMOTION, not just connectivity (hot standby answers reads while
# replay is still in flight — see Wait-Promoted).
Wait-Promoted $RecoveryPort 120
Note 'recovery: cluster promoted and accepting connections'

# --- 10. verify -------------------------------------------------------------
$phasesRaw = Invoke-Psql $RecoveryPort 'SELECT phase FROM drill ORDER BY id' 'mtaafix_pitr'
$phases = @($phasesRaw -split "`r?`n" | Where-Object { $_ })
$hasA = $phases -contains 'phase-a-before-base'
$hasB = $phases -contains 'phase-b-after-base'
$hasC = $phases -contains 'phase-c-must-not-recover'
Note "recovered rows: [$($phases -join ', ')]"
Note "assertions: A recovered=$hasA (expect True), B recovered=$hasB (expect True), C recovered=$hasC (expect False)"

if (-not ($hasA -and $hasB -and -not $hasC)) {
    Note 'DRILL FAILED - recovery did not stop at the target time'
    throw 'PITR drill failed: see assertions above'
}
Note 'DRILL PASSED - point-in-time recovery stopped between B and C'

# --- 11. evidence + cleanup -------------------------------------------------
Stop-Cluster $RecoveryPort $recovered 'recovered.log'
# Include the per-segment archive wrapper log: it is the primary evidence
# that archiving really ran (timestamp, source, post-copy size per segment).
if (Test-Path $script:wrapperLog) {
    Note '--- archive-command.log (one line per archived WAL segment) ---'
    foreach ($l in (Get-Content -LiteralPath $script:wrapperLog)) { Note $l }
}
if ($script:restoreLog -and (Test-Path $script:restoreLog)) {
    Note '--- restore-command.log (one line per WAL segment replayed in recovery) ---'
    foreach ($l in (Get-Content -LiteralPath $script:restoreLog)) { Note $l }
}
$evidence = $ev -join "`n"
Set-Content -Path $EvidenceFile -Value $evidence
Write-Host "`n===== EVIDENCE =====`n$evidence`n(evidence written to $EvidenceFile)"
if (-not $KeepWorkDir) { Remove-Item -Recurse -Force $WorkDir }
Write-Host 'RESULT: PITR DRILL PASSED'
