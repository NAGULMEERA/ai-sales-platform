<#
.SYNOPSIS
  Backup AI Sales Platform PostgreSQL databases (Windows).

.EXAMPLE
  $env:PGPASSWORD='aisales'; .\scripts\backup-postgres.ps1 -OutputDir .\backups\postgres
#>
param(
    [string]$OutputDir = "",
    [string]$PgHost = $(if ($env:PGHOST) { $env:PGHOST } else { "localhost" }),
    [int]$PgPort = $(if ($env:PGPORT) { [int]$env:PGPORT } else { 5432 }),
    [string]$PgUser = $(if ($env:PGUSER) { $env:PGUSER } else { "aisales" })
)

$ErrorActionPreference = "Stop"
if (-not $OutputDir) {
    $stamp = (Get-Date).ToUniversalTime().ToString("yyyyMMddTHHmmssZ")
    $OutputDir = Join-Path "backups/postgres" $stamp
}
New-Item -ItemType Directory -Force -Path $OutputDir | Out-Null

$databases = @(
    "aisales_identity", "aisales_tenant", "aisales_notification", "aisales_workflow",
    "lead_db", "catalog_db", "customer_db", "conversation_db", "deal_db", "ai_db", "marketplace_db",
    "appointment_db", "media_db", "billing_db", "search_db", "analytics_db", "audit_db", "integration_db"
)

Write-Host "Backing up to $OutputDir"
foreach ($db in $databases) {
    $exists = & psql -h $PgHost -p $PgPort -U $PgUser -d postgres -tAc "SELECT 1 FROM pg_database WHERE datname='$db'" 2>$null
    if ($exists -match "1") {
        Write-Host "Dumping $db..."
        & pg_dump -h $PgHost -p $PgPort -U $PgUser -d $db -Fc -f (Join-Path $OutputDir "$db.dump")
    } else {
        Write-Host "Skipping missing database: $db"
    }
}

$manifest = @(
    "created_at_utc=$((Get-Date).ToUniversalTime().ToString('o'))",
    "host=${PgHost}:${PgPort}",
    "user=$PgUser"
) + (Get-ChildItem $OutputDir -Filter *.dump | ForEach-Object { $_.Name })
$manifest | Set-Content (Join-Path $OutputDir "MANIFEST.txt")
Write-Host "Backup complete: $OutputDir"
