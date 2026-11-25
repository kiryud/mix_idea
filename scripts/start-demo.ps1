$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
Push-Location (Join-Path $taskRoot 'demo-web')
try {
    & ..\scripts\assert-prerequisites.ps1 -RequireNode
    if (-not (Test-Path -LiteralPath 'node_modules/.package-lock.json')) {
        & npm.cmd ci --no-audit --no-fund --cache (Join-Path $taskRoot '.local/npm-cache')
        if ($LASTEXITCODE -ne 0) { throw 'Demo dependency installation failed.' }
    }
    & npm.cmd run dev
    if ($LASTEXITCODE -ne 0) { throw 'Demo did not start. Check the port and displayed error.' }
} finally { Pop-Location }
