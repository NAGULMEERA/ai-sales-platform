# Scaffolds a platform-ready business microservice (Phase 4 — Platform Infrastructure Epic).
# Usage: .\scripts\scaffold-platform-service.ps1 -Name lead -Port 8083 -Database lead_db
param(
    [Parameter(Mandatory = $true)]
    [string]$Name,
    [Parameter(Mandatory = $true)]
    [int]$Port,
    [Parameter(Mandatory = $true)]
    [string]$Database,
    [string]$TenantTable = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$backend = Join-Path $root "backend"
$serviceName = "$Name-service"
$classPrefix = (Get-Culture).TextInfo.ToTitleCase($Name)
$appClass = "${classPrefix}ServiceApplication"
$base = Join-Path $backend "services/$serviceName"
$pkg = "com.aisales.$Name"
$pkgPath = $pkg.Replace('.', '/')
$table = if ($TenantTable) { $TenantTable } else { "${Name}s" }

function Write-TextFile($path, $content) {
    $dir = Split-Path $path -Parent
    if ($dir -and !(Test-Path $dir)) { New-Item -ItemType Directory -Force -Path $dir | Out-Null }
    $utf8NoBom = New-Object System.Text.UTF8Encoding $false
    [System.IO.File]::WriteAllText($path, $content, $utf8NoBom)
}

if (!(Test-Path $base)) {
    Write-Error "Service directory not found: $base. Run scaffold-service-structure.ps1 first."
}

# DDD package placeholders
$layers = @(
    "domain/entity",
    "domain/repository",
    "domain/service",
    "application/service",
    "application/mapper",
    "infrastructure/persistence",
    "infrastructure/configuration",
    "api/controller",
    "api/request",
    "api/response"
)
foreach ($layer in $layers) {
    $marker = Join-Path $base "src/main/java/$pkgPath/$layer/.gitkeep"
    Write-TextFile $marker ""
}

Write-TextFile (Join-Path $base "src/main/resources/application.yml") @"
server:
  port: $Port

spring:
  application:
    name: $serviceName
  config:
    import:
      - optional:configserver:http://localhost:8888
      - optional:classpath:platform/application-observability.yml
      - optional:classpath:platform/application-cache.yml
  datasource:
    url: jdbc:postgresql://localhost:5432/$Database
    username: aisales
    password: aisales
  jpa:
    hibernate:
      ddl-auto: validate
    open-in-view: false
  flyway:
    enabled: true
    locations: classpath:db/migration
  kafka:
    bootstrap-servers: localhost:9092

aisales:
  security:
    use-platform-defaults: true
  events:
    default-topic: aisales-events
    outbox:
      enabled: true
    inbox:
      enabled: true

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
"@

$migrationDir = Join-Path $base "src/main/resources/db/migration"
$nextVersion = (Get-ChildItem $migrationDir -Filter "V*.sql" | Measure-Object).Count + 1

Write-TextFile (Join-Path $migrationDir ("V{0:D3}__platform_rls_helpers.sql" -f $nextVersion)) @"
-- Platform Infrastructure Epic — RLS helper function (copy from common-core db/platform template).

CREATE OR REPLACE FUNCTION platform_enable_tenant_rls(
    p_schema text,
    p_table text,
    p_policy_name text DEFAULT NULL
)
RETURNS void
LANGUAGE plpgsql
AS `$`$`$
DECLARE
    v_policy text;
BEGIN
    v_policy := COALESCE(p_policy_name, p_table || '_tenant_isolation');
    EXECUTE format('ALTER TABLE %I.%I ENABLE ROW LEVEL SECURITY', p_schema, p_table);
    EXECUTE format('ALTER TABLE %I.%I FORCE ROW LEVEL SECURITY', p_schema, p_table);
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I.%I', v_policy, p_schema, p_table);
    EXECUTE format(
        'CREATE POLICY %I ON %I.%I USING (
            current_setting(''app.is_platform_admin'', true) = ''true''
            OR tenant_id = NULLIF(current_setting(''app.current_tenant'', true), '''')::uuid
        ) WITH CHECK (
            current_setting(''app.is_platform_admin'', true) = ''true''
            OR tenant_id = NULLIF(current_setting(''app.current_tenant'', true), '''')::uuid
        )',
        v_policy, p_schema, p_table
    );
END;
`$`$`$;
"@

Write-TextFile (Join-Path $migrationDir ("V{0:D3}__platform_outbox.sql" -f ($nextVersion + 1))) @"
CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    topic VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP,
    retry_count INT NOT NULL DEFAULT 0
);
CREATE INDEX IF NOT EXISTS idx_outbox_events_status_created ON outbox_events(status, created_at);
"@

Write-TextFile (Join-Path $migrationDir ("V{0:D3}__platform_inbox.sql" -f ($nextVersion + 2))) @"
CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(255) NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, consumer_name)
);
CREATE INDEX IF NOT EXISTS idx_processed_events_consumer ON processed_events(consumer_name, processed_at);

CREATE TABLE IF NOT EXISTS dead_letter (
    id UUID PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    partition_id INT,
    message_offset BIGINT,
    event_id VARCHAR(255),
    event_type VARCHAR(100),
    payload TEXT NOT NULL,
    error_message TEXT,
    consumer_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);
CREATE INDEX IF NOT EXISTS idx_dead_letter_consumer ON dead_letter(consumer_name, created_at);
"@

Write-TextFile (Join-Path $migrationDir ("V{0:D3}__platform_rls_$table.sql" -f ($nextVersion + 3))) @"
-- Enable RLS on primary tenant-owned aggregate table(s).
SELECT platform_enable_tenant_rls('public', '$table');
"@

Write-TextFile (Join-Path $base "PLATFORM.md") @"
# $serviceName — platform adoption

This service was scaffolded with `scaffold-platform-service.ps1`.

## Platform capabilities enabled

- Observability: `application-observability.yml`
- Cache (optional): `application-cache.yml` + `aisales.cache.enabled=true`
- Outbox publishing: `aisales.events.outbox.enabled=true`
- Inbox consumption: `aisales.events.inbox.enabled=true`
- RLS: `platform_enable_tenant_rls` Flyway migrations

## Next steps

1. Implement domain aggregate under `domain/`
2. Add REST controller under `api/controller/`
3. Publish integration events via `EventPublisher` (outbox)
4. Consume events with `IntegrationEventListener` in `@KafkaListener` methods

See `docs/03-architecture/platform-infrastructure-epic.md`.
"@

Write-Host "Platform scaffold applied to $serviceName (migrations V$nextVersion-V$($nextVersion + 3))."
Write-Host "Update pom.xml manually if common-events / spring-kafka are not yet present."
