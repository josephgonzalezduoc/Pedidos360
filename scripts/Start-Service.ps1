param(
    [Parameter(Mandatory = $true)]
    [ValidateSet('catalog-service', 'orders-service', 'notify-service', 'rabbit-admin-service')]
    [string]$Service,
    [string]$JavaHome
)
. "$PSScriptRoot\Common.ps1"
Import-PedidosEnvironment
Set-PedidosJava $JavaHome
Push-Location (Join-Path $ProjectRoot $Service)
try {
    & .\mvnw.cmd "-Dmaven.repo.local=$MavenRepo" spring-boot:run
    if ($LASTEXITCODE -ne 0) { throw "No se pudo ejecutar $Service. Revisa el error anterior." }
} finally { Pop-Location }
