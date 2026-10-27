# Phase 19 (B-16.3): backup automation - database (pg_dump custom format)
# plus the uploads/ media directory, timestamped into backend/scripts/backups
# with integrity hashes and age-based retention. PGPASSWORD must be supplied
# via the environment (never stored in the script or the repo).
#
# Usage:
#   $env:PGPASSWORD = '<db password>'
#   powershell -ExecutionPolicy Bypass -File scripts\backup.ps1
#   powershell -ExecutionPolicy Bypass -File scripts\backup.ps1 -RetentionDays 30
#
# Restore (into a scratch database, never the live one by default):
#   powershell -ExecutionPolicy Bypass -File scripts\restore-database.ps1 `
#     -DumpFile scripts\backups\mtaafix_20261023_120000.dump
param(
    [string]$PgHost = 'localhost',
    [int]$PgPort = 5432,
    [string]$PgUser = 'postgres',
    [string]$PgDb = 'mtaafix',
    [string]$UploadsDir = (Join-Path $PSScriptRoot '..\uploads'),
    [string]$OutDir = (Join-Path $PSScriptRoot 'backups'),
    [int]$RetentionDays = 14
)

$ErrorActionPreference = 'Stop'

if (-not $env:PGPASSWORD) {
    throw 'PGPASSWORD is not set - libpq tools would prompt interactively and hang. Set it for this shell session only.'
}
foreach ($tool in @('pg_dump')) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "$tool not found on PATH - install PostgreSQL client tools."
    }
}

New-Item -ItemType Directory -Force $OutDir | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd_HHmmss'

# --- 1. Database dump (custom format: compressed, pg_restore-able) ---------
$dumpFile = Join-Path $OutDir "mtaafix_$stamp.dump"
& pg_dump -h $PgHost -p $PgPort -U $PgUser -d $PgDb -Fc -f $dumpFile
if ($LASTEXITCODE -ne 0) { throw "pg_dump failed with exit code $LASTEXITCODE" }
Write-Host "[ok] database dump: $dumpFile"

# --- 2. Uploads archive -----------------------------------------------------
$uploadsZip = Join-Path $OutDir "uploads_$stamp.zip"
if (Test-Path $UploadsDir) {
    $items = Get-ChildItem -Path $UploadsDir -Recurse -File
    if ($items.Count -eq 0) {
        Set-Content -Path $uploadsZip -Value 'EMPTY (uploads directory has no files)'
        Write-Host "[ok] uploads archive: empty placeholder (no files yet)"
    } else {
        Compress-Archive -Path (Join-Path $UploadsDir '*') -DestinationPath $uploadsZip -Force
        Write-Host "[ok] uploads archive: $uploadsZip ($($items.Count) files)"
    }
} else {
    Set-Content -Path $uploadsZip -Value 'MISSING (uploads directory absent at backup time)'
    Write-Host "[warn] uploads directory not found - recorded as missing"
}

# --- 3. Manifest with sizes + SHA-256 integrity hashes ----------------------
$manifest = @(
    Get-Item $dumpFile, $uploadsZip | ForEach-Object {
        [pscustomobject]@{
            file   = $_.Name
            bytes  = $_.Length
            sha256 = (Get-FileHash -Algorithm SHA256 -Path $_.FullName).Hash.ToLower()
            utc    = (Get-Date).ToUniversalTime().ToString('o')
        }
    }
) | ConvertTo-Json
$manifestFile = Join-Path $OutDir "manifest_$stamp.json"
Set-Content -Path $manifestFile -Value $manifest
Write-Host "[ok] manifest: $manifestFile"

# --- 4. Retention pruning ---------------------------------------------------
$cutoff = (Get-Date).AddDays(-$RetentionDays)
$pruned = @(Get-ChildItem -Path $OutDir -File | Where-Object { $_.LastWriteTime -lt $cutoff })
$pruned | Remove-Item -Force
Write-Host "[ok] retention: pruned $($pruned.Count) file(s) older than $RetentionDays day(s)"

# --- 5. Summary -------------------------------------------------------------
$kept = @(Get-ChildItem -Path $OutDir -File | Sort-Object Name)
$kept | Format-Table Name, @{ n = 'MB'; e = { [math]::Round($_.Length / 1MB, 2) } } -AutoSize
Write-Host "RESULT: BACKUP OK ($($kept.Count) files in $OutDir)"
