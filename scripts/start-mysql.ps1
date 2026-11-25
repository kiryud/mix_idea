$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$taskMetadataFile = Join-Path $taskRoot '.local/mysql-dev-access.json'
$taskMetadata = Get-Content -LiteralPath $taskMetadataFile | ConvertFrom-Json
$taskExpectedData = [IO.Path]::GetFullPath((Join-Path $taskRoot '.local/mysql-dev'))
if ([IO.Path]::GetFullPath($taskMetadata.dataDirectory) -ne $taskExpectedData) {
    throw 'The project database directory does not match this workspace.'
}
$taskListener = Get-NetTCPConnection -State Listen -LocalPort $taskMetadata.port -ErrorAction SilentlyContinue
if ($taskListener) {
    $taskDbProcess = Get-CimInstance Win32_Process -Filter "ProcessId=$($taskListener[0].OwningProcess)"
    if ($taskDbProcess.CommandLine -notlike "*$taskExpectedData*") {
        throw 'The configured database port is used by another process.'
    }
    Write-Host "Project MySQL is ready on port $($taskMetadata.port)."
    return
}
$taskDbProcess = Start-Process -FilePath $taskMetadata.mysqld -ArgumentList @(
    '--no-defaults', "--datadir=$taskExpectedData", "--port=$($taskMetadata.port)",
    '--bind-address=127.0.0.1', '--mysqlx=0', '--console'
) -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $taskRoot '.local/mysql-dev-out.log') -RedirectStandardError (Join-Path $taskRoot '.local/mysql-dev-error.log')
Set-Content -LiteralPath (Join-Path $taskRoot '.local/mysql-dev.pid') -Value $taskDbProcess.Id
for ($taskAttempt = 0; $taskAttempt -lt 30; $taskAttempt++) {
    if (Get-NetTCPConnection -State Listen -LocalPort $taskMetadata.port -ErrorAction SilentlyContinue) {
        Write-Host "Project MySQL started on port $($taskMetadata.port)."
        return
    }
    if ($taskDbProcess.HasExited) { throw 'Project MySQL failed. Check .local/mysql-dev-error.log.' }
    Start-Sleep -Milliseconds 500
}
throw 'Project MySQL did not become ready in time.'
