param(
    [string]$ApiBase = "http://localhost:8080/api/v1",
    [string]$FrontendBase = "http://localhost:4200"
)

$ErrorActionPreference = "Stop"

function Assert-True {
    param(
        [bool]$Condition,
        [string]$Message
    )

    if (-not $Condition) {
        throw $Message
    }
}

Write-Host "=== SMOKE TEST (API + FRONTEND) ===" -ForegroundColor Cyan
Write-Host "API: $ApiBase"
Write-Host "Frontend: $FrontendBase"

# 1) Frontend / SPA
$frontCode = (Invoke-WebRequest -UseBasicParsing "$FrontendBase").StatusCode
$spaCode = (Invoke-WebRequest -UseBasicParsing "$FrontendBase/transferencias").StatusCode
Assert-True ($frontCode -eq 200) "Frontend '/' indisponivel (status $frontCode)."
Assert-True ($spaCode -eq 200) "Frontend '/transferencias' falhou (status $spaCode)."
Write-Host "[OK] Frontend e fallback SPA" -ForegroundColor Green

# 2) Swagger + Health
$swaggerCode = (Invoke-WebRequest -UseBasicParsing "http://localhost:8080/swagger-ui.html").StatusCode
$health = Invoke-RestMethod -Method GET -Uri "http://localhost:8080/actuator/health"
Assert-True ($swaggerCode -eq 200) "Swagger indisponivel (status $swaggerCode)."
Assert-True ($health.status -eq "UP") "Health endpoint nao esta UP."
Write-Host "[OK] Swagger + Health" -ForegroundColor Green

# 3) Lista para base
$lista = Invoke-RestMethod -Method GET -Uri "$ApiBase/beneficios"
Assert-True ($lista.Count -ge 2) "Necessario ao menos 2 beneficios para testar transferencia."

$origem = $lista[0]
$destino = $lista[1]
$valorTransfer = [decimal]1.00

# 4) Transferencia valida
$bodyTransfer = @{
    origemId = $origem.id
    destinoId = $destino.id
    valor = $valorTransfer
} | ConvertTo-Json

Invoke-WebRequest -UseBasicParsing -Method POST -Uri "$ApiBase/transferencias" -ContentType "application/json" -Body $bodyTransfer | Out-Null

$listaApos = Invoke-RestMethod -Method GET -Uri "$ApiBase/beneficios"
$origemApos = $listaApos | Where-Object { $_.id -eq $origem.id }
$destinoApos = $listaApos | Where-Object { $_.id -eq $destino.id }

$origemDebitou = (([decimal]$origem.valor - $valorTransfer) -eq ([decimal]$origemApos.valor))
$destinoCreditou = (([decimal]$destino.valor + $valorTransfer) -eq ([decimal]$destinoApos.valor))

Assert-True $origemDebitou "Transferencia nao debitou origem corretamente."
Assert-True $destinoCreditou "Transferencia nao creditou destino corretamente."
Write-Host "[OK] Transferencia valida" -ForegroundColor Green

# 5) Transferencia invalida: saldo insuficiente (422)
$erro422 = $false
try {
    $body422 = @{
        origemId = $origem.id
        destinoId = $destino.id
        valor = 9999999
    } | ConvertTo-Json

    Invoke-WebRequest -UseBasicParsing -Method POST -Uri "$ApiBase/transferencias" -ContentType "application/json" -Body $body422 | Out-Null
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 422) {
        $erro422 = $true
    }
}
Assert-True $erro422 "Esperava 422 para saldo insuficiente."
Write-Host "[OK] Validacao saldo insuficiente (422)" -ForegroundColor Green

# 6) Transferencia invalida: origem == destino (400)
$erro400 = $false
try {
    $body400 = @{
        origemId = $origem.id
        destinoId = $origem.id
        valor = 1
    } | ConvertTo-Json

    Invoke-WebRequest -UseBasicParsing -Method POST -Uri "$ApiBase/transferencias" -ContentType "application/json" -Body $body400 | Out-Null
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 400) {
        $erro400 = $true
    }
}
Assert-True $erro400 "Esperava 400 para origem igual destino."
Write-Host "[OK] Validacao origem igual destino (400)" -ForegroundColor Green

# 7) CRUD: create -> read -> update -> delete -> 404
$novo = @{
    nome = "SMOKE SCRIPT BENEFICIO"
    descricao = "Criado por scripts/smoke-test.ps1"
    valor = 10.10
    ativo = $true
} | ConvertTo-Json

$criado = Invoke-RestMethod -Method POST -Uri "$ApiBase/beneficios" -ContentType "application/json" -Body $novo
$id = $criado.id
Assert-True ($null -ne $id) "Create nao retornou ID."

$lido = Invoke-RestMethod -Method GET -Uri "$ApiBase/beneficios/$id"
Assert-True ($lido.id -eq $id) "Read por ID falhou."

$upd = @{
    nome = "SMOKE SCRIPT BENEFICIO UPD"
    descricao = "Atualizado por scripts/smoke-test.ps1"
    valor = 12.34
    ativo = $true
} | ConvertTo-Json

$atualizado = Invoke-RestMethod -Method PUT -Uri "$ApiBase/beneficios/$id" -ContentType "application/json" -Body $upd
Assert-True ($atualizado.nome -eq "SMOKE SCRIPT BENEFICIO UPD") "Update nao persistiu nome esperado."

Invoke-WebRequest -UseBasicParsing -Method DELETE -Uri "$ApiBase/beneficios/$id" | Out-Null

$deu404 = $false
try {
    Invoke-RestMethod -Method GET -Uri "$ApiBase/beneficios/$id" | Out-Null
} catch {
    if ($_.Exception.Response.StatusCode.value__ -eq 404) {
        $deu404 = $true
    }
}
Assert-True $deu404 "Esperava 404 apos delete logico."
Write-Host "[OK] CRUD completo" -ForegroundColor Green

Write-Host "SMOKE TEST: PASSOU" -ForegroundColor Green
