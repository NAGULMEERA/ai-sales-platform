$ErrorActionPreference = "Stop"
$root = Join-Path (Split-Path -Parent $PSScriptRoot) "backend\services"
$newServices = @(
    @{ Name = "search-service"; Port = 8094 },
    @{ Name = "media-service"; Port = 8095 },
    @{ Name = "audit-service"; Port = 8096 },
    @{ Name = "deal-service"; Port = 8097 },
    @{ Name = "marketplace-service"; Port = 8098 }
)

foreach ($svc in $newServices) {
    $dockerfile = @"
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/$($svc.Name)-*.jar app.jar
EXPOSE $($svc.Port)
ENTRYPOINT ["java", "-jar", "app.jar"]
"@
    $path = Join-Path $root "$($svc.Name)/Dockerfile"
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($path, $dockerfile, $utf8NoBom)
}
Write-Host "Created Dockerfiles for new services."
