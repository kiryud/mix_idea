$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$taskData = [IO.Path]::GetFullPath((Join-Path $taskRoot '.local/mysql-test'))
$taskMetadataPath = Join-Path $taskRoot '.local/mysql-test-access.json'
$taskBinary = (Get-Command mysqld.exe -ErrorAction SilentlyContinue).Source
if (-not $taskBinary) {
    $taskKnownBinary = 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysqld.exe'
    if (Test-Path -LiteralPath $taskKnownBinary) { $taskBinary = $taskKnownBinary }
}
if (-not $taskBinary) { throw 'MySQL 8 is required. Install MySQL or set IDEA_TEST_JDBC_URL and the test DB credentials.' }
$taskClient = Join-Path (Split-Path $taskBinary) 'mysql.exe'
$taskListener = Get-NetTCPConnection -State Listen -LocalPort 33307 -ErrorAction SilentlyContinue
if ($taskListener) {
    $taskOwner = Get-CimInstance Win32_Process -Filter "ProcessId=$($taskListener[0].OwningProcess)"
    if ($taskOwner.CommandLine -notlike "*$taskData*") { throw 'Port 33307 belongs to another process. It was left unchanged.' }
}
if (-not (Test-Path -LiteralPath $taskMetadataPath)) {
    if (Test-Path -LiteralPath (Join-Path $taskData 'mysql')) { throw 'Existing test data has no matching credentials. Set an explicit test URL instead.' }
    New-Item -ItemType Directory -Path $taskData -Force | Out-Null
    & $taskBinary --no-defaults "--datadir=$taskData" --initialize-insecure --console 2> (Join-Path $taskRoot '.local/mysql-test-init.log')
    if ($LASTEXITCODE -ne 0) { throw 'Test MySQL initialization failed.' }
    $taskRandom = [Convert]::ToHexString([Security.Cryptography.RandomNumberGenerator]::GetBytes(32))
    @{ user = 'idea_test'; password = $taskRandom; rootPassword = '' } | ConvertTo-Json | Set-Content -LiteralPath $taskMetadataPath
}
if (-not $taskListener) {
    # WMI starts a hidden independent process; Gradle must not inherit a long-running DB's streams.
    $taskStartup = New-CimInstance -ClassName Win32_ProcessStartup -ClientOnly -Property @{ ShowWindow = [uint16]0 }
    $taskLog = Join-Path $taskRoot '.local/mysql-test-error.log'
    $taskCommand = '"' + $taskBinary + '" --no-defaults --datadir="' + $taskData + '" --port=33307 --bind-address=127.0.0.1 --mysqlx=0 --log-error="' + $taskLog + '"'
    $taskCreated = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{ CommandLine = $taskCommand; ProcessStartupInformation = $taskStartup; CurrentDirectory = $taskRoot }
    if ($taskCreated.ReturnValue -ne 0) { throw 'Could not start the isolated test database.' }
}
$taskAccess = Get-Content -LiteralPath $taskMetadataPath | ConvertFrom-Json
if ($taskAccess.user -notmatch '^[A-Za-z0-9_]+$' -or $taskAccess.password -notmatch '^[A-Za-z0-9_-]+$') { throw 'Invalid local test credential format.' }
$taskOldPassword = $env:MYSQL_PWD
try {
    $env:MYSQL_PWD = $taskAccess.rootPassword
    $taskReady = $false
    for ($taskAttempt = 0; $taskAttempt -lt 40; $taskAttempt++) {
        & $taskClient --protocol=TCP -h 127.0.0.1 -P 33307 -u root --connect-timeout=1 -N -e 'SELECT 1' 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $taskReady = $true; break }
        Start-Sleep -Milliseconds 500
    }
    if (-not $taskReady) { throw 'Test MySQL did not become ready. See .local/mysql-test-error.log.' }
    $taskSql = "CREATE DATABASE IF NOT EXISTS idea_support_test CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci; CREATE USER IF NOT EXISTS '$($taskAccess.user)'@'127.0.0.1' IDENTIFIED BY '$($taskAccess.password)'; GRANT ALL ON idea_support_test.* TO '$($taskAccess.user)'@'127.0.0.1';"
    $taskSql | & $taskClient --protocol=TCP -h 127.0.0.1 -P 33307 -u root --connect-timeout=2
    if ($LASTEXITCODE -ne 0) { throw 'Test database preparation failed.' }
} finally { $env:MYSQL_PWD = $taskOldPassword }
Write-Host 'Isolated MySQL test database ready: 127.0.0.1:33307/idea_support_test'
