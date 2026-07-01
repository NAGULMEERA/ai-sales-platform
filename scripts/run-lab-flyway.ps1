# Start lab Postgres + run monolith Flyway migrations
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$labDir = Join-Path $root "backend/database"

Set-Location $labDir

Write-Host "Starting postgres-lab (pgvector, port 5433)..."
docker compose -f docker-compose-lab.yml up -d postgres-lab

Write-Host "Waiting for PostgreSQL..."
$ready = $false
for ($i = 0; $i - 30; $i++) {
    $status = docker inspect -f "{{.State.Health.Status}}" aisales-postgres-lab 2>$null
    if ($status -eq "healthy") { $ready = $true; break }
    Start-Sleep -Seconds 2
}
if (-not $ready) {
    throw "postgres-lab did not become healthy. Is Docker Desktop running?"
}

Write-Host "Running Flyway migrate..."
docker compose -f docker-compose-lab.yml --profile migrate run --rm flyway-monolith `
    -url=jdbc:postgresql://postgres-lab:5432/ai_sales_platform `
    -user=aisales -password=aisales -schemas=public `
    -locations=filesystem:/flyway/sql -baselineOnMigrate=true migrate

Write-Host "Migration complete. Run info:"
docker compose -f docker-compose-lab.yml run --rm flyway/flyway:10 `
    -url=jdbc:postgresql://postgres-lab:5432/ai_sales_platform `
    -user=aisales -password=aisales -schemas=public `
    -locations=filesystem:/flyway/sql info
