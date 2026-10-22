$ErrorActionPreference = 'Stop'
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$Services = @('catalog-service', 'orders-service', 'notify-service', 'rabbit-admin-service')

function Import-PedidosEnvironment {
    $envFile = Join-Path $ProjectRoot '.env'
    if (-not (Test-Path $envFile)) {
        throw 'Falta .env. Copia .env.example a .env y define las tres claves.'
    }
    foreach ($line in Get-Content -LiteralPath $envFile) {
        $text = $line.Trim()
        if (-not $text -or $text.StartsWith('#')) { continue }
        $parts = $text -split '=', 2
        if ($parts.Count -ne 2 -or $parts[0].Trim() -notmatch '^[A-Z][A-Z0-9_]*$') {
            throw 'Formato invalido en .env; usa NOMBRE=valor, una variable por linea.'
        }
        $key = $parts[0].Trim()
        $value = $parts[1].Trim()
        if ($value.Length -ge 2 -and (
            ($value.StartsWith('"') -and $value.EndsWith('"')) -or
            ($value.StartsWith("'") -and $value.EndsWith("'")))) {
            $value = $value.Substring(1, $value.Length - 2)
        }
        [Environment]::SetEnvironmentVariable($key, $value, 'Process')
    }
    foreach ($key in @('CATALOG_DB_PASSWORD', 'ORDERS_DB_PASSWORD', 'RABBITMQ_PASSWORD')) {
        $value = [Environment]::GetEnvironmentVariable($key, 'Process')
        if (-not $value -or $value.StartsWith('CAMBIAR_')) {
            throw "Define un valor para $key en .env."
        }
    }
}

function Set-PedidosJava([string]$JavaHome) {
    if (-not $JavaHome) { $JavaHome = $env:JAVA_HOME }
    if (-not $JavaHome -and (Test-Path 'F:\jdk-21\jdk-21.0.12.1+1')) {
        $JavaHome = 'F:\jdk-21\jdk-21.0.12.1+1'
    }
    if (-not $JavaHome) { throw 'Indica -JavaHome con la carpeta de tu JDK 21.' }
    $compiler = Join-Path $JavaHome 'bin\javac.exe'
    if (-not (Test-Path $compiler)) { throw 'JavaHome debe apuntar a un JDK con bin\javac.exe.' }
    $version = & $compiler -version
    if ($LASTEXITCODE -ne 0 -or "$version" -notmatch '^javac 21[. ]') {
        throw 'Este proyecto usa JDK 21. Indica -JavaHome con la ruta correcta.'
    }
    $env:JAVA_HOME = $JavaHome
    $env:Path = "$(Join-Path $JavaHome 'bin');$env:Path"
    $env:MAVEN_USER_HOME = Join-Path $ProjectRoot '.cache\maven-home'
    $script:MavenRepo = Join-Path $ProjectRoot '.cache\maven-repository'
}
