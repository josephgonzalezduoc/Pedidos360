param([string]$JavaHome)
. "$PSScriptRoot\Common.ps1"
Import-PedidosEnvironment
Set-PedidosJava $JavaHome
foreach ($service in $Services) {
    Push-Location (Join-Path $ProjectRoot $service)
    try {
        & .\mvnw.cmd "-Dmaven.repo.local=$MavenRepo" clean verify
        if ($LASTEXITCODE -ne 0) { throw "Fallo la verificacion de $service." }
    } finally { Pop-Location }
}
Write-Host 'Los cuatro servicios finalizaron clean verify correctamente.'
