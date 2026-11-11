param([string]$MySqlTestUrl = '')
$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
Push-Location $taskRoot
try {
    & .\scripts\assert-prerequisites.ps1 -RequireJava
    if ($MySqlTestUrl) { $env:IDEA_TEST_JDBC_URL = $MySqlTestUrl }
    $taskJavaTemp = (Join-Path $env:SystemRoot 'Temp').Replace('\', '/')
    if ($env:JAVA_TOOL_OPTIONS -notmatch 'jdk.net.unixdomain.tmpdir') {
        $env:JAVA_TOOL_OPTIONS = "$env:JAVA_TOOL_OPTIONS -Djdk.net.unixdomain.tmpdir=$taskJavaTemp -Djava.io.tmpdir=$taskJavaTemp"
    }
    & .\gradlew.bat test --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Tests failed. See build/reports/tests/test/index.html.' }
} finally { Pop-Location }
