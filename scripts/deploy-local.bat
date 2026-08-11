@echo off
setlocal EnableExtensions EnableDelayedExpansion

REM =============================================================================
REM Deploy AI Sales Platform locally (Windows CMD)
REM
REM Usage:
REM   cd /d D:\CursorProject\ai-sales-platform
REM   scripts\deploy-local.bat
REM   scripts\deploy-local.bat --skip-maven
REM   scripts\deploy-local.bat --infra-only
REM   scripts\deploy-local.bat --services-only
REM
REM Images are built from locally Maven-built jars (fast). Run Maven at least once
REM before --skip-maven:
REM   cd backend && mvnw.cmd clean install -DskipTests
REM =============================================================================

REM Capture script location BEFORE any "shift" (shift overwrites %%0 / %%~dp0).
set "SCRIPT_DIR=%~dp0"
pushd "%SCRIPT_DIR%.." >nul || (
  echo ERROR: Cannot resolve repo root from: %SCRIPT_DIR%
  exit /b 1
)
set "ROOT=%CD%"
popd >nul

set "BACKEND=%ROOT%\backend"
set "DEPLOYMENT=%ROOT%\deployment"
set "PREBUILT_DOCKERFILE=%DEPLOYMENT%\Dockerfile.prebuilt"

set "SKIP_MAVEN=0"
set "INFRA_ONLY=0"
set "SERVICES_ONLY=0"

:parse_args
if "%~1"=="" goto args_done
if /I "%~1"=="--skip-maven" set "SKIP_MAVEN=1"
if /I "%~1"=="--infra-only" set "INFRA_ONLY=1"
if /I "%~1"=="--services-only" set "SERVICES_ONLY=1"
if /I "%~1"=="--help" goto usage
if /I "%~1"=="-h" goto usage
shift /1
goto parse_args

:args_done

if not exist "%DEPLOYMENT%\docker-compose-infra.yml" (
  echo ERROR: Expected compose file not found:
  echo   %DEPLOYMENT%\docker-compose-infra.yml
  echo Script dir: %SCRIPT_DIR%
  echo Resolved root: %ROOT%
  exit /b 1
)

if not exist "%PREBUILT_DOCKERFILE%" (
  echo ERROR: Missing %PREBUILT_DOCKERFILE%
  exit /b 1
)

cd /d "%ROOT%" || (
  echo ERROR: Cannot cd to repo root: %ROOT%
  exit /b 1
)

echo.
echo === AI Sales Platform local deploy ===
echo Root: %ROOT%
echo.

where docker >nul 2>&1
if errorlevel 1 (
  echo ERROR: docker not found in PATH. Start Docker Desktop and retry.
  exit /b 1
)

docker info >nul 2>&1
if errorlevel 1 (
  echo ERROR: Docker engine is not running. Start Docker Desktop and retry.
  exit /b 1
)

echo [1/4] Ensuring Docker network aisales-network ...
docker network create aisales-network >nul 2>&1
if errorlevel 1 (
  echo       Network already exists.
) else (
  echo       Network created.
)

if "%SERVICES_ONLY%"=="1" goto build_maven

echo [2/4] Starting infrastructure ...
docker compose -f "%DEPLOYMENT%\docker-compose-infra.yml" up -d
if errorlevel 1 (
  echo ERROR: Infra compose failed.
  echo Tip: if you see network errors, run:
  echo   docker compose -f deployment\docker-compose-infra.yml down
  echo   docker rm -f aisales-postgres aisales-redis aisales-mailpit aisales-zipkin
  echo   then re-run this script.
  exit /b 1
)

echo       Waiting 15s for Postgres/Kafka ...
timeout /t 15 /nobreak >nul

if "%INFRA_ONLY%"=="1" goto done_infra

:build_maven
if "%SKIP_MAVEN%"=="1" (
  echo [3/4] Skipping Maven build (--skip-maven^)
  goto start_services
)

echo [3/4] Maven clean install -DskipTests ...
cd /d "%BACKEND%" || (
  echo ERROR: backend folder missing.
  exit /b 1
)
call .\mvnw.cmd clean install -DskipTests
if errorlevel 1 (
  echo ERROR: Maven build failed.
  exit /b 1
)
cd /d "%ROOT%"

:start_services
echo [4/4] Building runtime images from local jars (one at a time^) ...
echo.

call :build_prebuilt aisales/service-registry:local     infrastructure\service-registry\target     service-registry-1.0.0-SNAPSHOT.jar     8761
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/config-server:local        infrastructure\config-server\target        config-server-1.0.0-SNAPSHOT.jar        8888
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/api-gateway:local          infrastructure\api-gateway\target          api-gateway-1.0.0-SNAPSHOT.jar          8080
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/identity-service:local     backend\services\identity-service\target     identity-service-1.0.0-SNAPSHOT.jar     8081
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/tenant-service:local       backend\services\tenant-service\target       tenant-service-1.0.0-SNAPSHOT.jar       8082
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/lead-service:local         backend\services\lead-service\target         lead-service-1.0.0-SNAPSHOT.jar         8083
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/customer-service:local     backend\services\customer-service\target     customer-service-1.0.0-SNAPSHOT.jar     8084
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/catalog-service:local      backend\services\catalog-service\target      catalog-service-1.0.0-SNAPSHOT.jar      8085
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/conversation-service:local backend\services\conversation-service\target conversation-service-1.0.0-SNAPSHOT.jar 8086
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/ai-service:local           backend\services\ai-service\target           ai-service-1.0.0-SNAPSHOT.jar           8088
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/workflow-service:local     backend\services\workflow-service\target     workflow-service-1.0.0-SNAPSHOT.jar     8089
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/notification-service:local backend\services\notification-service\target notification-service-1.0.0-SNAPSHOT.jar 8090
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/billing-service:local      backend\services\billing-service\target      billing-service-1.0.0-SNAPSHOT.jar      8091
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/integration-service:local  backend\services\integration-service\target  integration-service-1.0.0-SNAPSHOT.jar  8092
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/analytics-service:local    backend\services\analytics-service\target    analytics-service-1.0.0-SNAPSHOT.jar    8093
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/search-service:local       backend\services\search-service\target       search-service-1.0.0-SNAPSHOT.jar       8094
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/media-service:local        backend\services\media-service\target        media-service-1.0.0-SNAPSHOT.jar        8095
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/deal-service:local         backend\services\deal-service\target         deal-service-1.0.0-SNAPSHOT.jar         8097
if errorlevel 1 exit /b 1
call :build_prebuilt aisales/marketplace-service:local  backend\services\marketplace-service\target  marketplace-service-1.0.0-SNAPSHOT.jar  8098
if errorlevel 1 exit /b 1

echo --- Starting containers ---
docker compose -f "%DEPLOYMENT%\docker-compose-services.yml" up -d --no-build
if errorlevel 1 (
  echo ERROR: Services compose failed.
  exit /b 1
)

echo.
echo === Deploy complete ===
echo Gateway:     http://localhost:8080
echo Eureka:      http://localhost:8761
echo Config:      http://localhost:8888
echo Mailpit UI:  http://localhost:8025
echo Grafana:     http://localhost:3000
echo.
echo Check status:
echo   docker ps
exit /b 0

:done_infra
echo.
echo === Infra only - complete ===
echo Postgres:  localhost:5433
echo Redis:     localhost:6379
echo Kafka:     localhost:9092
echo Mailpit:   http://localhost:8025
exit /b 0

:usage
echo Usage: scripts\deploy-local.bat [options]
echo.
echo Options:
echo   --skip-maven      Skip backend\mvnw.cmd clean install
echo   --infra-only      Only start docker-compose-infra.yml
echo   --services-only   Skip infra; package jars into images + start services
echo   --help            Show this help
exit /b 0

REM -----------------------------------------------------------------------------
REM :build_prebuilt IMAGE JAR_DIR JAR_NAME PORT
REM -----------------------------------------------------------------------------
:build_prebuilt
set "IMAGE=%~1"
set "JAR_DIR=%ROOT%\%~2"
set "JAR_NAME=%~3"
set "PORT=%~4"
set "JAR_FILE=%JAR_DIR%\%JAR_NAME%"

echo --- Building %IMAGE% ---
if not exist "%JAR_FILE%" (
  echo ERROR: Jar not found: %JAR_FILE%
  echo Run Maven first:
  echo   cd /d %BACKEND%
  echo   mvnw.cmd clean install -DskipTests
  exit /b 1
)

docker build -f "%PREBUILT_DOCKERFILE%" --build-arg "JAR_NAME=%JAR_NAME%" --build-arg "SERVER_PORT=%PORT%" -t "%IMAGE%" "%JAR_DIR%"
if errorlevel 1 (
  echo ERROR: Docker build failed for %IMAGE%
  exit /b 1
)
exit /b 0
