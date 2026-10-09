[CmdletBinding()]
param()

$ErrorActionPreference = 'Continue'
$repoRoot = Split-Path -Parent $PSScriptRoot
$runtimeDir = Join-Path $repoRoot '.local'

foreach ($name in 'frontend', 'gateway', 'aggregation') {
    $pidFile = Join-Path $runtimeDir "$name.pid"
    if (-not (Test-Path $pidFile)) { continue }

    $trackedPid = [int](Get-Content $pidFile -Raw).Trim()
    $process = Get-Process -Id $trackedPid -ErrorAction SilentlyContinue
    if ($process) {
        try {
            Stop-Process -Id $trackedPid -ErrorAction Stop
            Write-Host "[OK] Stopped $name (PID $trackedPid)"
        }
        catch {
            Write-Warning "Could not stop $name (PID $trackedPid): $($_.Exception.Message)"
            continue
        }
    }
    Remove-Item -LiteralPath $pidFile -Force -ErrorAction SilentlyContinue
}

$mysqlAdmin = Join-Path $env:ProgramFiles 'MySQL\MySQL Server 8.0\bin\mysqladmin.exe'
if (Test-Path $mysqlAdmin) {
    & $mysqlAdmin --protocol=tcp --host=127.0.0.1 --port=3307 --user=root --password=root shutdown 2>$null
    if ($LASTEXITCODE -eq 0) { Write-Host '[OK] Stopped local MySQL on port 3307' }
}

Write-Host '[INFO] Memurai was left running because it is a shared Windows service.'
