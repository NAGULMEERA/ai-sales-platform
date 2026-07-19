<#
.SYNOPSIS
  Generates optimized multi-stage Dockerfiles for every Spring Boot service.

.DESCRIPTION
  Build context is the repository root:
    docker build -f backend/services/lead-service/Dockerfile -t aisales/lead-service:latest .

  Does not change business logic. Overwrites Dockerfiles in place with a consistent template.
#>
$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot

$services = @(
    @{ Path = "backend/services/identity-service"; Module = "services/identity-service"; Jar = "identity-service"; Port = 8081 },
    @{ Path = "backend/services/tenant-service"; Module = "services/tenant-service"; Jar = "tenant-service"; Port = 8082 },
    @{ Path = "backend/services/lead-service"; Module = "services/lead-service"; Jar = "lead-service"; Port = 8083 },
    @{ Path = "backend/services/customer-service"; Module = "services/customer-service"; Jar = "customer-service"; Port = 8084 },
    @{ Path = "backend/services/catalog-service"; Module = "services/catalog-service"; Jar = "catalog-service"; Port = 8085 },
    @{ Path = "backend/services/conversation-service"; Module = "services/conversation-service"; Jar = "conversation-service"; Port = 8086 },
    @{ Path = "backend/services/appointment-service"; Module = "services/appointment-service"; Jar = "appointment-service"; Port = 8087 },
    @{ Path = "backend/services/ai-service"; Module = "services/ai-service"; Jar = "ai-service"; Port = 8088 },
    @{ Path = "backend/services/workflow-service"; Module = "services/workflow-service"; Jar = "workflow-service"; Port = 8089 },
    @{ Path = "backend/services/notification-service"; Module = "services/notification-service"; Jar = "notification-service"; Port = 8090 },
    @{ Path = "backend/services/billing-service"; Module = "services/billing-service"; Jar = "billing-service"; Port = 8091 },
    @{ Path = "backend/services/integration-service"; Module = "services/integration-service"; Jar = "integration-service"; Port = 8092 },
    @{ Path = "backend/services/analytics-service"; Module = "services/analytics-service"; Jar = "analytics-service"; Port = 8093 },
    @{ Path = "backend/services/search-service"; Module = "services/search-service"; Jar = "search-service"; Port = 8094 },
    @{ Path = "backend/services/media-service"; Module = "services/media-service"; Jar = "media-service"; Port = 8095 },
    @{ Path = "backend/services/audit-service"; Module = "services/audit-service"; Jar = "audit-service"; Port = 8096 },
    @{ Path = "backend/services/deal-service"; Module = "services/deal-service"; Jar = "deal-service"; Port = 8097 },
    @{ Path = "backend/services/marketplace-service"; Module = "services/marketplace-service"; Jar = "marketplace-service"; Port = 8098 },
    @{ Path = "infrastructure/api-gateway"; Module = "../infrastructure/api-gateway"; Jar = "api-gateway"; Port = 8080 },
    @{ Path = "infrastructure/config-server"; Module = "../infrastructure/config-server"; Jar = "config-server"; Port = 8888 },
    @{ Path = "infrastructure/service-registry"; Module = "../infrastructure/service-registry"; Jar = "service-registry"; Port = 8761 },
    @{ Path = "infrastructure/discovery-service"; Module = "../infrastructure/discovery-service"; Jar = "discovery-service"; Port = 8762 }
)

function Get-DockerfileContent($module, $jar, $port) {
    return @"
# syntax=docker/dockerfile:1.7
# Multi-stage image for $jar
# Build from repository root:
#   docker build -f $($module -replace '^\.\./','')/Dockerfile -t aisales/${jar}:latest .
# (For backend modules use: -f backend/$module/Dockerfile with context = repo root)

FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

RUN apk add --no-cache bash git

COPY backend/mvnw backend/pom.xml ./backend/
COPY backend/.mvn ./backend/.mvn
COPY backend/common ./backend/common
COPY backend/plugins ./backend/plugins
COPY backend/services ./backend/services
COPY infrastructure ./infrastructure

WORKDIR /workspace/backend
RUN chmod +x mvnw \
 && ./mvnw -pl $module -am package -DskipTests -B \
 && cp $module/target/${jar}-*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S aisales && adduser -S -G aisales aisales \
 && apk add --no-cache curl \
 && rm -rf /var/cache/apk/*

COPY --from=builder /workspace/app.jar /app/app.jar
RUN chown aisales:aisales /app/app.jar

USER aisales
EXPOSE $port

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    SERVER_PORT=$port

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:${port}/actuator/health/liveness" || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
"@
}

$utf8 = New-Object System.Text.UTF8Encoding $false
foreach ($svc in $services) {
    $dockerfilePath = Join-Path $root (Join-Path $svc.Path "Dockerfile")
    $dir = Split-Path $dockerfilePath -Parent
    if (-not (Test-Path $dir)) {
        Write-Host "Skip missing dir: $($svc.Path)"
        continue
    }
    # Fix path comment for infra modules
    $content = Get-DockerfileContent $svc.Module $svc.Jar $svc.Port
    if ($svc.Path.StartsWith("infrastructure/")) {
        $content = $content -replace 'backend/\$module/Dockerfile', "$($svc.Path)/Dockerfile"
        $content = $content -replace '\(For backend modules use:.*?\)\r?\n', ""
    }
    [System.IO.File]::WriteAllText($dockerfilePath, $content, $utf8)
    Write-Host "Wrote $dockerfilePath"
}

Write-Host "Done. Generated $($services.Count) Dockerfiles."
