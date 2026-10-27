# Phase 19 (B-16.3): restore a pg_dump custom-format backup into a scratch
# database and show what landed. Deliberately refuses to touch the live
# database unless you explicitly name it AND pass -Force.
#
# Usage:
#   $env:PGPASSWORD = '<db password>'
#   powershell -ExecutionPolicy Bypass -File scripts\restore-database.ps1 `
#     -DumpFile scripts\backups\mtaafix_<stamp>.dump
#   ... -TargetDb mtaafix -Force      # overwrite the live DB (destructive!)
param(
    [Parameter(Mandatory = $true)]
    [string]$DumpFile,
    [string]$PgHost = 'localhost',
    [int]$PgPort = 5432,
    [string]$PgUser = 'postgres',
    [string]$TargetDb = 'mtaafix_restore',
    [switch]$Force
)

$ErrorActionPreference = 'Stop'

if (-not $env:PGPASSWORD) {
    throw 'PGPASSWORD is not set - libpq tools would prompt interactively and hang.'
}
if (-not (Test-Path $DumpFile)) { throw "Dump file not found: $DumpFile" }
foreach ($tool in @('createdb', 'dropdb', 'pg_restore', 'psql')) {
    if (-not (Get-Command $tool -ErrorAction SilentlyContinue)) {
        throw "$tool not found on PATH - install PostgreSQL client tools."
    }
}

# --- 1. Target database -----------------------------------------------------
$exists = & psql -h $PgHost -p $PgPort -U $PgUser -d postgres -tAc `
    "SELECT 1 FROM pg_database WHERE datname = '$TargetDb'"
$exists = "$exists".Trim()
if ($exists -eq '1') {
    if ($TargetDb -eq 'mtaafix' -and -not $Force) {
        throw 'Refusing to drop the live database mtaafix without -Force.'
    }
    if (-not $Force) {
        throw "Database '$TargetDb' exists - pass -Force to drop and recreate it."
    }
    & dropdb -h $PgHost -p $PgPort -U $PgUser --if-exists $TargetDb
    if ($LASTEXITCODE -ne 0) { throw "dropdb failed with exit code $LASTEXITCODE" }
    Write-Host "[ok] dropped existing '$TargetDb'"
}
& createdb -h $PgHost -p $PgPort -U $PgUser $TargetDb
if ($LASTEXITCODE -ne 0) { throw "createdb failed with exit code $LASTEXITCODE" }
Write-Host "[ok] created database '$TargetDb'"

# --- 2. Restore -------------------------------------------------------------
& pg_restore -h $PgHost -p $PgPort -U $PgUser -d $TargetDb $DumpFile
if ($LASTEXITCODE -ne 0) { throw "pg_restore failed with exit code $LASTEXITCODE" }
Write-Host "[ok] pg_restore completed"

# --- 3. What landed? --------------------------------------------------------
$top = & psql -h $PgHost -p $PgPort -U $PgUser -d $TargetDb -c @"
SELECT relname AS table, n_live_tup AS approx_rows
FROM pg_stat_user_tables
ORDER BY n_live_tup DESC
LIMIT 5;
"@
Write-Host "Restored content (top tables by rows):"
$top | ForEach-Object { Write-Host $_ }

Write-Host "RESULT: RESTORE OK into '$TargetDb' from $DumpFile"
