# Consolidates identity and tenant services to com.aisales.{context} DDD packages.
$ErrorActionPreference = "Stop"
$root = "d:\CursorProject\ai-sales-platform"

$pathsToRemove = @(
    "$root\services\identity-service\src\main\java\com\aisales\services",
    "$root\services\tenant-service\src\main\java\com\aisales\services",
    "$root\services\identity-service\src\main\java\com\aisales\identity\controller",
    "$root\services\identity-service\src\main\java\com\aisales\identity\service",
    "$root\services\identity-service\src\main\java\com\aisales\identity\domain",
    "$root\services\identity-service\src\main\java\com\aisales\identity\repository",
    "$root\services\identity-service\src\main\java\com\aisales\identity\dto",
    "$root\services\identity-service\src\main\java\com\aisales\identity\exception",
    "$root\services\identity-service\src\main\java\com\aisales\identity\config",
    "$root\services\tenant-service\src\main\java\com\aisales\tenant\controller",
    "$root\services\tenant-service\src\main\java\com\aisales\tenant\service",
    "$root\services\tenant-service\src\main\java\com\aisales\tenant\domain",
    "$root\services\tenant-service\src\main\java\com\aisales\tenant\repository",
    "$root\services\tenant-service\src\main\java\com\aisales\tenant\dto",
    "$root\services\tenant-service\src\main\java\com\aisales\tenant\mapper",
    "$root\services\tenant-service\src\main\java\com\aisales\tenant\config",
    "$root\services\tenant-service\src\main\java\com\aisales\tenant\exception"
)

foreach ($p in $pathsToRemove) {
    if (Test-Path $p) { Remove-Item -Recurse -Force $p }
}

Write-Host "Removed legacy identity/tenant package trees."
