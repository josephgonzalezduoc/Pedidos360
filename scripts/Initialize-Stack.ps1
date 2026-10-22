param([switch]$Apps)
. "$PSScriptRoot\Common.ps1"
Import-PedidosEnvironment
if (-not $env:RABBITMQ_ERLANG_COOKIE -or $env:RABBITMQ_ERLANG_COOKIE.StartsWith('CAMBIAR_')) {
    $env:RABBITMQ_ERLANG_COOKIE = [guid]::NewGuid().ToString('N')
    $envFile = Join-Path $ProjectRoot '.env'
    $text = [IO.File]::ReadAllText($envFile)
    $keyLine = 'RABBITMQ_ERLANG_COOKIE=' + $env:RABBITMQ_ERLANG_COOKIE
    if ($text -match '(?m)^[ \t]*RABBITMQ_ERLANG_COOKIE=') {
        $text = [regex]::Replace($text, '(?m)^[ \t]*RABBITMQ_ERLANG_COOKIE=.*$', $keyLine)
    } else {
        $text = $text.TrimEnd() + [Environment]::NewLine + $keyLine + [Environment]::NewLine
    }
    [IO.File]::WriteAllText($envFile, $text, (New-Object Text.UTF8Encoding($false)))
    Write-Host 'Cookie privada creada en .env para los dos nodos. No se muestra ni se publica.'
}
if (-not $env:RABBITMQ_ERLANG_COOKIE -or
    $env:RABBITMQ_ERLANG_COOKIE.StartsWith('CAMBIAR_') -or
    $env:RABBITMQ_ERLANG_COOKIE.Length -lt 20) {
    throw 'Agrega RABBITMQ_ERLANG_COOKIE a .env con una cadena aleatoria de al menos 20 caracteres.'
}
Push-Location $ProjectRoot
try {
    docker compose -f compose.stack.yml up -d --wait --wait-timeout 240 rabbit1 rabbit2
    if ($LASTEXITCODE -ne 0) { throw 'No arrancaron ambos nodos. Revisar logs de compose.stack.yml.' }
    docker compose -f compose.stack.yml run --rm --no-deps rabbit-setup
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo verificar el cluster o importar la topologia.' }
    docker compose -f compose.stack.yml exec -T rabbit1 rabbitmqctl cluster_status
    if ($LASTEXITCODE -ne 0) { throw 'No se pudo consultar cluster_status.' }
    if ($Apps) {
        docker compose -f compose.stack.yml --profile apps up -d --build
        if ($LASTEXITCODE -ne 0) { throw 'Fallo el build o arranque de aplicaciones.' }
        Write-Host 'Esperar el arranque y verificar health en localhost:8091, 8092, 8093 y 8094.'
    }
    docker compose -f compose.stack.yml --profile apps ps -a
} finally { Pop-Location }
