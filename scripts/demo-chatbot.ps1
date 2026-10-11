<#
.SYNOPSIS
    Script de demonstration de bout en bout de l'Assistant RH IA d'AppRH.
.DESCRIPTION
    Verifie le frontend, authentifie un utilisateur, puis enchaine :
      1. question de comptage (reponse deterministe),
      2. streaming NDJSON,
      3. persistance serveur de l'historique (GET/DELETE),
      4. metriques Actuator,
      5. (option) limite de debit HTTP 429.
    La pile Docker doit etre demarree (docker compose up -d).
.EXAMPLE
    .\scripts\demo-chatbot.ps1
.EXAMPLE
    .\scripts\demo-chatbot.ps1 -IncludeRateLimit
#>
[CmdletBinding()]
param(
    [string]$FrontendUrl = "http://localhost:4200",
    [string]$ApiUrl = "http://localhost:8081/api",
    [string]$Email = "employe@apprh.local",
    [string]$Password = "Apprh@2026!",
    [switch]$IncludeRateLimit
)

$ErrorActionPreference = "Stop"
$script:failures = 0

function Write-Section([string]$title) {
    Write-Host ""
    Write-Host ("=" * 60) -ForegroundColor DarkGray
    Write-Host " $title" -ForegroundColor Cyan
    Write-Host ("=" * 60)
}

function Write-Ok([string]$message) {
    Write-Host "  [OK]   $message" -ForegroundColor Green
}

function Write-Fail([string]$message) {
    $script:failures++
    Write-Host "  [FAIL] $message" -ForegroundColor Red
}

function Invoke-Json {
    param([string]$Method, [string]$Uri, [hashtable]$Headers, $Body)
    $params = @{ Method = $Method; Uri = $Uri; Headers = $headers }
    if ($null -ne $body) {
        $json = $Body | ConvertTo-Json -Depth 6 -Compress
        $bytes = [System.Text.Encoding]::UTF8.GetBytes($json)
        $params.Body = $bytes
        $params.ContentType = "application/json; charset=utf-8"
    }
    Invoke-RestMethod @params
}

Write-Section "1. Frontend"
try {
    $resp = Invoke-WebRequest $FrontendUrl -UseBasicParsing
    Write-Host "   HTTP $($resp.StatusCode) sur $FrontendUrl"
    if ($resp.StatusCode -eq 200) { Write-Host "   [OK] frontend disponible" -ForegroundColor Green }
    else { Write-Host "   [!!] statut inattendu" -ForegroundColor Yellow; $script:failures++ }
} catch {
    Write-Host "   [ECHEC] frontend injoignable : $($_.Exception.Message)" -ForegroundColor Red
    $script:failures++
}

Write-Section "Authentification ($Email)"
$login = Invoke-RestMethod -Method Post -Uri "$ApiUrl/auth/login" `
    -Body ([System.Text.Encoding]::UTF8.GetBytes((@{ email = $Email; password = $Password } | ConvertTo-Json))) `
    -ContentType "application/json; charset=utf-8"
$token = $login.accessToken
$headers = @{ Authorization = "Bearer $token" }
if ([string]::IsNullOrWhiteSpace($token)) { throw "Jeton JWT non obtenu." }
Write-Host "   [OK] jeton JWT obtenu (role $($login.user.role))" -ForegroundColor Green

Write-Section "1. Reponse deterministe (comptage conges)"
$chat = Invoke-RestMethod -Method Post -Uri "$ApiUrl/chatbot/chat" -Headers $headers `
    -Body ([System.Text.Encoding]::UTF8.GetBytes((@{ message = "Combien de conges approuves ai-je ?" } | ConvertTo-Json))) `
    -ContentType "application/json; charset=utf-8"
Write-Host "   aiAvailable=$($chat.aiAvailable) reply=""$($chat.reply)"""
if ($chat.reply -match "\d") { Write-Host "   [OK] reponse chiffree" -ForegroundColor Green }
else { Write-Host "   [!!] reponse sans chiffre" -ForegroundColor Yellow; $script:failures++ }

Write-Section "2. Streaming NDJSON"
$tmp = Join-Path $env:TEMP "apprh-chat-$([guid]::NewGuid()).json"
[System.IO.File]::WriteAllText($tmp, (@{ message = "Combien de taches ai-je ?" } | ConvertTo-Json), [System.Text.Encoding]::UTF8)
try {
    $stream = & curl.exe -N -s --data-binary "@$tmp" -H "Content-Type: application/json" `
        -H "Authorization: Bearer $token" "$ApiUrl/chatbot/chat/stream"
    $stream | ForEach-Object { Write-Host "   $_" }
    if ($stream -match '"type":"token"' -and $stream -match '"type":"done"') {
        Write-Host "   [OK] flux NDJSON token/done recu" -ForegroundColor Green
    } else {
        Write-Host "   [!!] flux NDJSON incomplet" -ForegroundColor Yellow; $script:failures++
    }
} finally {
    Remove-Item $tmp -ErrorAction SilentlyContinue
}

Write-Section "3. Persistance serveur de l'historique"
$cleared = Invoke-WebRequest -Method Delete -Uri "$ApiUrl/chatbot/messages" -Headers $headers -UseBasicParsing
Write-Host "   DELETE /chatbot/messages -> HTTP $($cleared.StatusCode)"
Invoke-RestMethod -Method Post -Uri "$ApiUrl/chatbot/chat" -Headers $headers `
    -Body ([System.Text.Encoding]::UTF8.GetBytes((@{ message = "Combien de conges approuves ai-je ?" } | ConvertTo-Json))) `
    -ContentType "application/json; charset=utf-8" | Out-Null
$messages = Invoke-RestMethod -Method Get -Uri "$ApiUrl/chatbot/messages" -Headers $headers
Write-Host "   messages persistes : $($messages.Count) (roles : $(($messages | ForEach-Object { $_.role }) -join ', '))"
if ($messages.Count -eq 2) { Write-Host "   [OK] question + reponse persistees" -ForegroundColor Green }
else { Write-Host "   [!!] nombre de messages inattendu" -ForegroundColor Yellow; $script:failures++ }

Write-Section "4. Metriques Actuator"
try {
    $metric = Invoke-RestMethod -Method Get -Uri "$ApiUrl/actuator/metrics/chatbot.requests" -Headers $headers
    $count = ($metric.measurements | Where-Object { $_.statistic -eq "COUNT" }).value
    Write-Host "   $($metric.name) (COUNT=$count)"
    Write-Host "   [OK] metrique exposee" -ForegroundColor Green
} catch {
    Write-Host "   [ECHEC] metrique indisponible : $($_.Exception.Message)" -ForegroundColor Red
    $script:failures++
}

if ($IncludeRateLimit) {
    Write-Section "5. Limite de debit (HTTP 429)"
    Write-Host "   Envoi de requetes jusqu'a obtenir un 429..."
    $rateLimited = $false
    for ($i = 1; $i -le 40; $i++) {
        try {
            Invoke-WebRequest -Method Post -Uri "$ApiUrl/chatbot/chat" -Headers $headers `
                -Body ([System.Text.Encoding]::UTF8.GetBytes((@{ message = "Combien de conges approuves ai-je ?" } | ConvertTo-Json))) `
                -ContentType "application/json; charset=utf-8" -UseBasicParsing | Out-Null
        } catch {
            $code = $_.Exception.Response.StatusCode.value__
            if ($code -eq 429) { $rateLimited = $true; Write-Host "   [OK] 429 recu apres $i requetes" -ForegroundColor Green; break }
        }
    }
    if (-not $rateLimited) { Write-Host "   [!!] 429 non atteint (quota superieur a 40)" -ForegroundColor Yellow; $script:failures++ }
}

Write-Host ""
if ($script:failures -eq 0) {
    Write-Host "DEMO REUSSIE : toutes les verifications sont passees." -ForegroundColor Green
    exit 0
} else {
    Write-Host "DEMO TERMINEE AVEC $($script:failures) ECHEC(S)." -ForegroundColor Red
    exit 1
}
