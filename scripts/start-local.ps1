[CmdletBinding()]
param()

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$repoRoot = Split-Path -Parent $PSScriptRoot
$runtimeDir = Join-Path $repoRoot '.local'
$sentinelLogDir = Join-Path $runtimeDir 'sentinel-logs'
New-Item -ItemType Directory -Path $runtimeDir, $sentinelLogDir -Force | Out-Null

function Test-LocalPort {
    param([Parameter(Mandatory)][int]$Port)

    $client = [System.Net.Sockets.TcpClient]::new()
    try {
        $connection = $client.ConnectAsync('127.0.0.1', $Port)
        return $connection.Wait(1000) -and $client.Connected
    }
    catch {
        return $false
    }
    finally {
        $client.Dispose()
    }
}

function Wait-LocalPort {
    param(
        [Parameter(Mandatory)][int]$Port,
        [Parameter(Mandatory)][string]$Name,
        [int]$TimeoutSeconds = 45
    )

    $deadline = (Get-Date).AddSeconds($TimeoutSeconds)
    while ((Get-Date) -lt $deadline) {
        if (Test-LocalPort -Port $Port) {
            Write-Host "[OK] $Name is listening on $Port"
            return
        }
        Start-Sleep -Milliseconds 500
    }
    throw "$Name did not start on port $Port within $TimeoutSeconds seconds."
}

function Find-Java17 {
    $adoptiumRoot = Join-Path $env:ProgramFiles 'Eclipse Adoptium'
    if (Test-Path $adoptiumRoot) {
        $jdk = Get-ChildItem $adoptiumRoot -Directory -Filter 'jdk-17*' |
            Sort-Object Name -Descending |
            Select-Object -First 1
        if ($jdk) {
            $java = Join-Path $jdk.FullName 'bin\java.exe'
            if (Test-Path $java) { return $java }
        }
    }
    throw 'JDK 17 was not found under Program Files\Eclipse Adoptium.'
}

function Start-TrackedProcess {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$FilePath,
        [Parameter(Mandatory)][string[]]$ArgumentList,
        [Parameter(Mandatory)][string]$WorkingDirectory,
        [Parameter(Mandatory)][int]$Port
    )

    if (Test-LocalPort -Port $Port) {
        Write-Host "[OK] $Name is already listening on $Port"
        return
    }

    $stdout = Join-Path $runtimeDir "$Name.out.log"
    $stderr = Join-Path $runtimeDir "$Name.err.log"
    $process = Start-Process -FilePath $FilePath -ArgumentList $ArgumentList `
        -WorkingDirectory $WorkingDirectory -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput $stdout -RedirectStandardError $stderr
    Set-Content -Path (Join-Path $runtimeDir "$Name.pid") -Value $process.Id
    Wait-LocalPort -Port $Port -Name $Name
}

# Redis-compatible service. Memurai is the Redis-recommended native Windows option.
if (-not (Test-LocalPort -Port 6379)) {
    $memurai = Get-Service -Name 'Memurai' -ErrorAction SilentlyContinue
    if (-not $memurai) { throw 'Memurai is not installed.' }
    Start-Service -Name 'Memurai'
}
Wait-LocalPort -Port 6379 -Name 'redis'

$redisCli = Join-Path $env:ProgramFiles 'Memurai\memurai-cli.exe'
if (Test-Path $redisCli) {
    $redisReply = & $redisCli -a 123456 ping 2>$null
    if ($LASTEXITCODE -ne 0 -or $redisReply -ne 'PONG') {
        throw 'Redis authentication failed. Expected password: 123456.'
    }
}

# Isolated MySQL instance for this repository. It does not touch the existing MySQL80 service.
if (-not (Test-LocalPort -Port 3307)) {
    $mysqlRoot = Join-Path $env:ProgramFiles 'MySQL\MySQL Server 8.0'
    $mysqld = Join-Path $mysqlRoot 'bin\mysqld.exe'
    $mysqlData = Join-Path $runtimeDir 'mysql-data'
    if (-not (Test-Path $mysqld)) { throw 'MySQL Server 8.0 was not found.' }
    if (-not (Test-Path (Join-Path $mysqlData 'auto.cnf'))) {
        throw 'The local MySQL data directory is missing. Re-run the one-time database setup.'
    }

    $mysqlArguments = @(
        '--no-defaults',
        ('--basedir="{0}"' -f ($mysqlRoot -replace '\\', '/')),
        ('--datadir="{0}"' -f ($mysqlData -replace '\\', '/')),
        '--port=3307',
        '--bind-address=127.0.0.1',
        '--mysqlx=0',
        ('--pid-file="{0}"' -f ((Join-Path $runtimeDir 'mysql-3307.pid') -replace '\\', '/')),
        ('--log-error="{0}"' -f ((Join-Path $runtimeDir 'mysql-3307.log') -replace '\\', '/'))
    )
    Start-Process -FilePath $mysqld -ArgumentList $mysqlArguments -WindowStyle Hidden | Out-Null
}
Wait-LocalPort -Port 3307 -Name 'mysql'

$java = Find-Java17
$env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $java)
$aggregationJar = Join-Path $repoRoot 'aggregation\target\shortlink-aggregation.jar'
$gatewayJar = Join-Path $repoRoot 'gateway\target\shortlink-gateway.jar'
if (-not (Test-Path $aggregationJar) -or -not (Test-Path $gatewayJar)) {
    Write-Host '[INFO] Backend jars are missing; building the project...'
    $mavenCommand = Get-Command mvn.cmd -ErrorAction Stop
    & $mavenCommand.Source '-DskipTests' '-Dspotless.apply.skip=true' package
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }
}

Start-TrackedProcess -Name 'aggregation' -FilePath $java `
    -ArgumentList @("-Dcsp.sentinel.log.dir=$sentinelLogDir", '-jar', $aggregationJar, '--spring.profiles.active=local') `
    -WorkingDirectory $repoRoot -Port 8003

Start-TrackedProcess -Name 'gateway' -FilePath $java `
    -ArgumentList @('-jar', $gatewayJar, '--spring.profiles.active=local') `
    -WorkingDirectory $repoRoot -Port 8000

$frontendRoot = Join-Path $repoRoot 'console-vue'
$vite = Join-Path $frontendRoot 'node_modules\vite\bin\vite.js'
if (-not (Test-Path $vite)) {
    Write-Host '[INFO] Frontend dependencies are missing; running npm install...'
    Push-Location $frontendRoot
    try { & npm.cmd install } finally { Pop-Location }
    if ($LASTEXITCODE -ne 0) { throw 'npm install failed.' }
}
$node = (Get-Command node.exe -ErrorAction Stop).Source
Start-TrackedProcess -Name 'frontend' -FilePath $node `
    -ArgumentList @($vite, '--host', '127.0.0.1') `
    -WorkingDirectory $frontendRoot -Port 5173

Write-Host ''
Write-Host 'ShortLink is ready:'
Write-Host '  Web:     http://127.0.0.1:5173/login'
Write-Host '  Account: admin / admin123456'
Write-Host '  Gateway: http://127.0.0.1:8000'
Write-Host '  Health:  http://127.0.0.1:8003/actuator/health'
