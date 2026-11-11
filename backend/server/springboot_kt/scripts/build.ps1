$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$taskOldJavaOptions = $env:JAVA_TOOL_OPTIONS
Push-Location $taskRoot
try {
    & .\scripts\assert-prerequisites.ps1 -RequireJava -RequireNode
    $taskJavaTemp = (Join-Path $env:SystemRoot 'Temp').Replace('\', '/')
    if ($env:JAVA_TOOL_OPTIONS -notmatch 'jdk.net.unixdomain.tmpdir') {
        $env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Djdk.net.unixdomain.tmpdir=$taskJavaTemp -Djava.io.tmpdir=$taskJavaTemp"
    }
    & .\gradlew.bat build --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Build failed. Review the failed test names and build/reports/tests/test/index.html.' }
    Write-Host 'Backend and React build succeeded. Test and coverage reports are ready.'
} finally { $env:JAVA_TOOL_OPTIONS = $taskOldJavaOptions; Pop-Location }
