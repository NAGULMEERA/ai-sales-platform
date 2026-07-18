# Start identity-service for local Postman testing (Windows).
# Email path: Identity outbox → Kafka → notification-service → Mailpit.
# Usage: .\scripts\run-identity-local.ps1

$ErrorActionPreference = "Stop"

$Root = Split-Path -Parent $PSScriptRoot
$Deployment = Join-Path $Root "deployment"
$Backend = Join-Path $Root "backend"

function Ensure-Database {
    param([string]$DatabaseName)
    $exists = docker exec aisales-postgres psql -U aisales -d aisales -tAc "SELECT 1 FROM pg_database WHERE datname = '$DatabaseName';"
    if ($exists -ne "1") {
        docker exec aisales-postgres psql -U aisales -d aisales -c "CREATE DATABASE $DatabaseName;"
        Write-Host "Created database $DatabaseName" -ForegroundColor Green
    } else {
        Write-Host "Database $DatabaseName already exists" -ForegroundColor Green
    }
}

Write-Host "Step 1: Starting Postgres + Kafka + Mailpit..." -ForegroundColor Cyan
docker compose -f (Join-Path $Deployment "docker-compose-infra.yml") up -d postgres kafka mailpit

Write-Host "Waiting for Postgres (15s)..." -ForegroundColor Gray
Start-Sleep -Seconds 15

Write-Host "Step 2: Ensuring databases exist..." -ForegroundColor Cyan
Ensure-Database "aisales_identity"
Ensure-Database "aisales_notification"
Ensure-Database "aisales_workflow"

Write-Host "Step 3: Starting notification-service in a new window (port 8090, profile=local, SMTP→Mailpit)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "Set-Location '$Backend'; .\mvnw.cmd spring-boot:run -pl services/notification-service `"-Dspring-boot.run.profiles=local`""
)

Write-Host "Step 4: Starting workflow-service in a new window (onboarding orchestration)..." -ForegroundColor Cyan
Start-Process powershell -ArgumentList @(
    "-NoExit",
    "-Command",
    "Set-Location '$Backend'; .\mvnw.cmd spring-boot:run -pl services/workflow-service `"-Dspring-boot.run.profiles=local`""
)

Write-Host "Waiting for notification-service + Kafka (25s)..." -ForegroundColor Gray
Start-Sleep -Seconds 25

Write-Host "Step 5: Starting identity-service (profile=local, port 8081)..." -ForegroundColor Cyan
Write-Host "  Swagger:     http://localhost:8081/swagger-ui.html" -ForegroundColor Gray
Write-Host "  Mailpit UI:  http://localhost:8025" -ForegroundColor Gray
Write-Host "  Postman:     backend/services/identity-service/postman/identity-service.postman_collection.json" -ForegroundColor Gray
Set-Location $Backend
& .\mvnw.cmd spring-boot:run -pl services/identity-service "-Dspring-boot.run.profiles=local"
