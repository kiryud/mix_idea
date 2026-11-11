param(
    [switch]$RequireJava,
    [switch]$RequireNode,
    [switch]$RequireDocker
)

$ErrorActionPreference = 'Stop'

function Get-ToolPath([string]$Name) {
    $command = Get-Command $Name -ErrorAction SilentlyContinue
    if (-not $command) {
        throw "'$Name' was not found in PATH. Install it and open a new PowerShell window."
    }
    return $command.Source
}

if ($RequireJava) {
	$taskRoot = Split-Path $PSScriptRoot -Parent
	$taskBundledJava = Get-ChildItem -Path (Join-Path $taskRoot '.local/jdk17') -Filter 'java.exe' -Recurse -ErrorAction SilentlyContinue |
		Select-Object -First 1
	if ($taskBundledJava) {
		$taskJavaHome = Split-Path (Split-Path $taskBundledJava.FullName -Parent) -Parent
		if (Test-Path -LiteralPath (Join-Path $taskJavaHome 'bin/javac.exe')) {
			$env:JAVA_HOME = $taskJavaHome
			$env:Path = "$taskJavaHome\bin;$env:Path"
		}
	}
	$javaPath = Get-ToolPath 'java'
	$javaVersionText = & $javaPath -version 2>&1 | Out-String
	if ($javaVersionText -notmatch '(?:version )?"?(17)(?:[.\s"]|$)') {
		throw "Java 17 JDK is required. Detected: $($javaVersionText.Trim())"
	}
	Get-ToolPath 'javac' | Out-Null
}

if ($RequireNode) {
	$nodePath = Get-ToolPath 'node'
	$nodeVersion = (& $nodePath --version).Trim()
	if ($nodeVersion -notmatch '^v(\d+)') {
		throw "Could not determine the Node.js version: $nodeVersion"
	}
	if ([int]$Matches[1] -lt 20) {
		throw "Node.js 20 or later is required. Detected: $nodeVersion"
	}
	Get-ToolPath 'npm.cmd' | Out-Null
}

if ($RequireDocker) {
	$taskRoot = Split-Path $PSScriptRoot -Parent
	$taskBundledDocker = Get-ChildItem -Path (Join-Path $taskRoot '.local/docker-cli') -Filter 'docker.exe' -Recurse -ErrorAction SilentlyContinue |
		Select-Object -First 1
	if ($taskBundledDocker) {
		$taskDockerDirectory = Split-Path $taskBundledDocker.FullName -Parent
		$env:Path = "$taskDockerDirectory;$env:Path"
		$env:DOCKER_CONFIG = Join-Path $taskRoot '.local/docker-config'
		New-Item -ItemType Directory -Force -Path $env:DOCKER_CONFIG | Out-Null
	}
	Get-ToolPath 'docker' | Out-Null
    & docker version --format '{{.Server.Version}}' 2>$null | Out-Null
    if ($LASTEXITCODE -ne 0) {
        throw 'Docker is installed but its engine is not running. Start Docker Desktop and retry.'
    }
}
