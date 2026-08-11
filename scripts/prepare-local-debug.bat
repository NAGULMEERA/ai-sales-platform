@echo off
setlocal EnableExtensions

REM Start infra only + create Postgres DBs used by application-local.yml profiles.
REM Then debug services from IntelliJ (Active profiles = local).

set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul
set "ROOT=%CD%"
popd >nul

set "DEPLOYMENT=%ROOT%\deployment"

echo === Prepare local debug (infra + databases) ===
echo.

docker info >nul 2>&1
if errorlevel 1 (
  echo ERROR: Start Docker Desktop first.
  exit /b 1
)

echo [1/3] Starting Postgres, Kafka, Redis, Mailpit ...
docker compose -f "%DEPLOYMENT%\docker-compose-infra.yml" up -d postgres kafka redis mailpit
if errorlevel 1 (
  echo ERROR: infra failed
  exit /b 1
)

echo [2/3] Waiting for Postgres ...
timeout /t 12 /nobreak >nul

echo [3/3] Ensuring databases exist ...
for %%D in (
  aisales_identity
  aisales_tenant
  aisales_notification
  aisales_workflow
  lead_db
  customer_db
  catalog_db
  conversation_db
  ai_db
  billing_db
  deal_db
  marketplace_db
  media_db
  search_db
  analytics_db
  integration_db
  appointment_db
  audit_db
) do (
  docker exec aisales-postgres psql -U aisales -d aisales -tAc "SELECT 1 FROM pg_database WHERE datname='%%D'" | findstr /R "1" >nul
  if errorlevel 1 (
    echo   Creating %%D ...
    docker exec aisales-postgres psql -U aisales -d aisales -c "CREATE DATABASE %%D;"
  ) else (
    echo   %%D already exists
  )
)

echo.
echo Infra ready. In IntelliJ:
echo   1. Open backend/ as the Maven project (or the repo root if already imported)
echo   2. Run/Debug any "* (local debug)" configuration
echo   3. Active profile must be: local
echo.
echo Do NOT start the same service in Docker while debugging it in IntelliJ (port clash).
echo Existing configs: backend\.run\
exit /b 0
