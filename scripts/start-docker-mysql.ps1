param([Parameter(Mandatory = $true)][string]$EnvFile)

$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent

function Get-EnvSetting([string]$Name, [string]$Default) {
	$line = Get-Content -LiteralPath $EnvFile | Where-Object { $_ -match "^$([regex]::Escape($Name))=(.*)$" } | Select-Object -First 1
	if (-not $line) { return $Default }
	$value = $line.Substring($Name.Length + 1)
	return $(if ($value) { $value } else { $Default })
}

$taskDbPassword = Get-EnvSetting 'DB_PASSWORD' 'local-db-password'
$taskRootPassword = Get-EnvSetting 'MYSQL_ROOT_PASSWORD' 'local-root-password'
$taskPort = Get-EnvSetting 'MYSQL_PORT' '3307'
$taskContainer = 'idea-support-mysql'
$taskExisting = & docker container inspect $taskContainer --format '{{.State.Running}}' 2>$null
if ($LASTEXITCODE -eq 0 -and $taskExisting -eq 'true') {
	Write-Host "Project MySQL is already running on port $taskPort."
	return
}
if ($LASTEXITCODE -eq 0) { & docker container start $taskContainer | Out-Null } else {
	& docker volume create idea-support-mysql-data | Out-Null
	& docker run --detach --name $taskContainer --restart unless-stopped --publish "127.0.0.1:$taskPort`:3306" --env 'MYSQL_DATABASE=idea_support' --env 'MYSQL_USER=idea_support' --env "MYSQL_PASSWORD=$taskDbPassword" --env "MYSQL_ROOT_PASSWORD=$taskRootPassword" --volume 'idea-support-mysql-data:/var/lib/mysql' --health-cmd 'MYSQL_PWD=$$MYSQL_PASSWORD mysql -u $$MYSQL_USER -e "SELECT 1" idea_support' --health-interval 5s --health-timeout 3s --health-retries 30 mysql:8.0 | Out-Null
}
for ($taskAttempt = 0; $taskAttempt -lt 36; $taskAttempt++) {
	$taskHealth = & docker container inspect $taskContainer --format '{{.State.Health.Status}}' 2>$null
	if ($taskHealth -eq 'healthy') { Write-Host "Project MySQL is ready on port $taskPort."; return }
	if ($taskHealth -eq 'unhealthy') { throw 'Project MySQL became unhealthy. Check docker logs idea-support-mysql.' }
	Start-Sleep -Seconds 2
}
throw 'Project MySQL did not become ready in time.'
