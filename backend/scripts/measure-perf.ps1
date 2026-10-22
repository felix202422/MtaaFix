# Phase 17 (NFR-7) performance measurement.
# Measures p50/p95/max for the dominant list endpoints against a live local
# server with the seeded ~5,000-report dataset. Client-side timing via
# Stopwatch (the RequestTimingFilter also reports X-Response-Time-Ms).
#
# Prereqs:
#   1. mvn package -DskipTests
#   2. Seed:  psql -U postgres -d mtaafix -f scripts/seed-perf.sql
#      (run scripts\seed-perf.ps1 or the psql command above after registering
#       perf-admin@mtaafix.local - see below; the script registers+promotes
#       the user on first run)
#   3. Server on :8085 (target\smoke-run.bat)
#
# Usage:  powershell -ExecutionPolicy Bypass -File scripts\measure-perf.ps1

param(
    [string]$BaseUrl = 'http://localhost:8085',
    [int]$Warmup = 5,
    [int]$Iterations = 30,
    [string]$PerfEmail = 'perf-admin@mtaafix.local',
    [string]$PerfPassword = 'PerfAdmin-2026!'
)

$ErrorActionPreference = 'Stop'

function Get-AuthHeaders([string]$token) {
    @{ Authorization = "Bearer $token"; Accept = 'application/json' }
}

function Invoke-JsonPost([string]$path, [hashtable]$body, [hashtable]$headers = @{ Accept = 'application/json' }) {
    $json = $body | ConvertTo-Json -Compress
    Invoke-RestMethod -Uri "$BaseUrl$path" -Method Post -Body $json `
        -ContentType 'application/json' -Headers $headers
}

# --- 0. Server up? ---------------------------------------------------------
try {
    $null = Invoke-RestMethod -Uri "$BaseUrl/actuator/health" -TimeoutSec 3
    Write-Host "[ok] server reachable at $BaseUrl"
} catch {
    throw "Server not reachable at $BaseUrl - start target\smoke-run.bat first."
}

# --- 1. Ensure the perf admin exists ---------------------------------------
$token = $null
try {
    $login = Invoke-JsonPost '/api/v1/auth/login' @{ email = $PerfEmail; password = $PerfPassword }
    $token = $login.accessToken
    Write-Host "[ok] logged in as $PerfEmail"
} catch {
    Write-Host "[info] $PerfEmail not found - registering..."
    try {
        $null = Invoke-JsonPost '/api/v1/auth/register' @{
            email = $PerfEmail; password = $PerfPassword
            fullName = 'Perf Admin'; role = 'CITIZEN'
        }
    } catch {
        Write-Host "[warn] register returned: $($_.Exception.Message) (continuing)"
    }
    Start-Sleep -Seconds 1
    $login = Invoke-JsonPost '/api/v1/auth/login' @{ email = $PerfEmail; password = $PerfPassword }
    $token = $login.accessToken
    # Promote to ADMINISTRATOR for staff/dashboard endpoints.
    $env:PGPASSWORD = 'Fizzo2025'
    & psql -U postgres -d mtaafix -c "UPDATE users SET role = 'ADMINISTRATOR' WHERE email = '$PerfEmail';" | Out-Null
    $login = Invoke-JsonPost '/api/v1/auth/login' @{ email = $PerfEmail; password = $PerfPassword }
    $token = $login.accessToken
    Write-Host "[ok] $PerfEmail promoted to ADMINISTRATOR"
}

# --- 1b. Ensure a perf field worker exists (assignments/me is FIELD_WORKER) --
$workerEmail = 'perf-worker@mtaafix.local'
$workerToken = $null
try {
    $wlogin = Invoke-JsonPost '/api/v1/auth/login' @{ email = $workerEmail; password = $PerfPassword }
    $workerToken = $wlogin.accessToken
} catch {
    try {
        $null = Invoke-JsonPost '/api/v1/auth/register' @{
            email = $workerEmail; password = $PerfPassword
            fullName = 'Perf Worker'
        }
    } catch {
        Write-Host "[warn] worker register: $($_.Exception.Message) (continuing)"
    }
    Start-Sleep -Seconds 1
    $env:PGPASSWORD = 'Fizzo2025'
    & psql -U postgres -d mtaafix -c "UPDATE users SET role = 'FIELD_WORKER' WHERE email = '$workerEmail';" | Out-Null
    $wlogin = Invoke-JsonPost '/api/v1/auth/login' @{ email = $workerEmail; password = $PerfPassword }
    $workerToken = $wlogin.accessToken
    Write-Host "[ok] $workerEmail created as FIELD_WORKER"
}
$worker = Get-AuthHeaders $workerToken

# --- 2. Dataset present? ----------------------------------------------------
$env:PGPASSWORD = 'Fizzo2025'
$perfCount = & psql -U postgres -d mtaafix -t -A -c `
    "SELECT count(*) FROM reports WHERE code LIKE 'MTF-2026-9%'"
$perfCount = [int]("$perfCount").Trim()
if ($perfCount -lt 4000) {
    throw "Only $perfCount perf reports found - run scripts\seed-perf.sql first."
}
Write-Host "[ok] dataset: $perfCount perf reports (total via public stats below)"

# --- 3. Endpoints under test -----------------------------------------------
$admin = Get-AuthHeaders $token
$anon = @{ Accept = 'application/json' }

$endpoints = @(
    @{ name = 'GET /reports/public (list, size=20)';      path = '/api/v1/reports/public?page=0&size=20';         headers = $anon }
    @{ name = 'GET /reports/public (map, size=100)';      path = '/api/v1/reports/public?page=0&size=100';        headers = $anon }
    @{ name = 'GET /reports/public (bbox filter)';       path = '/api/v1/reports/public?bbox=39.0,-7.0,40.0,-6.0&page=0&size=100'; headers = $anon }
    @{ name = 'GET /reports/public (q search)';          path = '/api/v1/reports/public?q=pothole&page=0&size=20'; headers = $anon }
    @{ name = 'GET /reports (staff list, size=20)';      path = '/api/v1/reports?page=0&size=20';                 headers = $admin }
    @{ name = 'GET /reports (staff status filter)';      path = '/api/v1/reports?status=SUBMITTED&page=0&size=20'; headers = $admin }
    @{ name = 'GET /categories/public';                  path = '/api/v1/categories/public';                      headers = $anon }
    @{ name = 'GET /stats/public (cached)';              path = '/api/v1/stats/public';                           headers = $anon }
    @{ name = 'GET /dashboard/admin (cached)';           path = '/api/v1/dashboard/admin';                        headers = $admin }
    @{ name = 'GET /assignments/me';                     path = '/api/v1/assignments/me?page=0&size=20';          headers = $worker }
    @{ name = 'GET /notifications (size=20)';            path = '/api/v1/notifications?page=0&size=20';           headers = $admin }
)

# --- 4. Measure -------------------------------------------------------------
function Measure-Endpoint([hashtable]$ep) {
    $times = New-Object System.Collections.Generic.List[double]
    $statusOk = $true
    $serverHeaderSeen = $false

    for ($i = 0; $i -lt ($Warmup + $Iterations); $i++) {
        $sw = [System.Diagnostics.Stopwatch]::StartNew()
        try {
            $resp = Invoke-WebRequest -Uri "$BaseUrl$($ep.path)" -Method Get `
                -Headers $ep.headers -UseBasicParsing -TimeoutSec 30
            $sw.Stop()
            if ($i -ge $Warmup) {
                $times.Add($sw.Elapsed.TotalMilliseconds)
                if ($resp.Headers['X-Response-Time-Ms']) { $serverHeaderSeen = $true }
            }
        } catch {
            $sw.Stop()
            $statusOk = $false
            if ($i -ge $Warmup) { $times.Add($sw.Elapsed.TotalMilliseconds) }
            Write-Host "[warn] $($ep.name) iteration $i failed: $($_.Exception.Message)"
        }
    }

    $sorted = $times | Sort-Object
    $n = $sorted.Count
    if ($n -eq 0) { return $null }
    $p50 = $sorted[[int][math]::Floor(0.50 * ($n - 1))]
    $p95 = $sorted[[int][math]::Floor(0.95 * ($n - 1))]
    $max = $sorted[$n - 1]
    $min = $sorted[0]
    [pscustomobject]@{
        Endpoint = $ep.name
        N        = $n
        P50_ms   = [math]::Round($p50, 1)
        P95_ms   = [math]::Round($p95, 1)
        Max_ms   = [math]::Round($max, 1)
        Min_ms   = [math]::Round($min, 1)
        Pass     = $statusOk -and ($p95 -lt 500)
        ServerHdr = $serverHeaderSeen
    }
}

Write-Host "`nMeasuring ($Warmup warmup + $Iterations iterations per endpoint)...`n"
$results = foreach ($ep in $endpoints) { Measure-Endpoint $ep }

$results | Format-Table -AutoSize

$pass = ($results | Where-Object { $_.Pass }).Count
$total = $results.Count
Write-Host "NFR-7 (p95 < 500ms list queries): $pass/$total endpoints pass"

$allPass = ($results | Where-Object { -not $_.Pass }).Count -eq 0
$summary = $results | ConvertTo-Json -Depth 3
Set-Content -Path (Join-Path $PSScriptRoot 'perf-results.json') -Value $summary
Write-Host "Results written to scripts\perf-results.json"

if (-not $allPass) {
    Write-Host "RESULT: SOME ENDPOINTS OVER THRESHOLD" -ForegroundColor Yellow
    exit 1
}
Write-Host "RESULT: ALL ENDPOINTS WITHIN p95 < 500 ms" -ForegroundColor Green
