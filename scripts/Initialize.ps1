. "$PSScriptRoot\Common.ps1"
Import-PedidosEnvironment
Push-Location $ProjectRoot
try {
    docker compose up -d --wait
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo iniciar la infraestructura.' }
    docker compose ps
} finally { Pop-Location }
