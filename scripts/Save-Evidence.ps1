param(
    [string]$ManagementUrl = 'http://localhost:15674',
    [switch]$Apps
)
. "$PSScriptRoot\Common.ps1"
Import-PedidosEnvironment
$pair = 'pedidos360:' + $env:RABBITMQ_PASSWORD
$headers = @{ Authorization = 'Basic ' + [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($pair)) }
$output = Join-Path $ProjectRoot ('evidence\' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
New-Item -ItemType Directory -Force -Path $output | Out-Null

$nodes = (Invoke-RestMethod -Uri "$ManagementUrl/api/nodes" -Headers $headers -TimeoutSec 15)
$safeNodes = @($nodes | Select-Object name, running, type, mem_used, disk_free)
ConvertTo-Json -InputObject $safeNodes -Depth 8 | Set-Content (Join-Path $output 'nodes.json') -Encoding UTF8
if ($nodes.Count -ne 2 -or @($nodes | Where-Object { -not $_.running }).Count -gt 0) {
    throw 'La API no muestra exactamente dos nodos activos. Revisar nodes.json.'
}

$queues = (Invoke-RestMethod -Uri "$ManagementUrl/api/queues/%2F" -Headers $headers -TimeoutSec 15)
$safeQueues = @($queues | Select-Object name, node, type, durable, messages_ready,
    messages_unacknowledged, consumers, policy, effective_policy_definition)
ConvertTo-Json -InputObject $safeQueues -Depth 10 | Set-Content (Join-Path $output 'queues.json') -Encoding UTF8

$policies = (Invoke-RestMethod -Uri "$ManagementUrl/api/policies/%2F" -Headers $headers -TimeoutSec 15)
ConvertTo-Json -InputObject $policies -Depth 10 | Set-Content (Join-Path $output 'policies.json') -Encoding UTF8

$exchanges = (Invoke-RestMethod -Uri "$ManagementUrl/api/exchanges/%2F" -Headers $headers -TimeoutSec 15)
$safeExchanges = @($exchanges | Where-Object { $_.name -like 'cmd.*' } |
    Select-Object name, type, durable)
ConvertTo-Json -InputObject $safeExchanges -Depth 6 | Set-Content (Join-Path $output 'exchanges.json') -Encoding UTF8

foreach ($domain in @('email', 'kitchen', 'invoice')) {
    if (@($queues | Where-Object { $_.name -eq "q.cmd.$domain" }).Count -ne 1 -or
        @($queues | Where-Object { $_.name -eq "q.cmd.$domain.dlq" }).Count -ne 1) {
        throw "Falta la cola o DLQ de $domain."
    }
}
foreach ($name in @('cmd-retention', 'dlq-retention')) {
    if (@($policies | Where-Object { $_.name -eq $name }).Count -ne 1) {
        throw "Falta la politica $name."
    }
}
if ($Apps) {
    $health = @()
    foreach ($port in @(8091, 8092, 8093, 8094)) {
        $response = Invoke-RestMethod -Uri "http://localhost:$port/actuator/health" -TimeoutSec 15
        if ($response.status -ne 'UP') { throw "Health del puerto $port no esta UP." }
        $health += [pscustomobject]@{ port = $port; status = $response.status }
    }
    ConvertTo-Json -InputObject $health | Set-Content (Join-Path $output 'health.json') -Encoding UTF8
}
Write-Host "Evidencias reales guardadas en $output"

