# Generate lead-service production migrations from monolith (no cross-service FKs)
param(
    [string]$MonolithDir = "backend/database/migrations-monolith",
    [string]$OutputDir = "backend/database/service-splits/lead-service"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$monolith = Join-Path $root $MonolithDir
$out = Join-Path $root $OutputDir
New-Item -ItemType Directory -Force -Path $out | Out-Null

$v001 = @"
-- Lead Service: foundation subset (production)
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TYPE lead_status AS ENUM (
    'NEW', 'CONTACTED', 'QUALIFIED', 'APPOINTMENT_BOOKED',
    'VISITED', 'NEGOTIATING', 'WON', 'LOST'
);

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS `$`$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
`$`$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_lead_external_id()
RETURNS TRIGGER AS `$`$
BEGIN
    NEW.external_id = 'LD-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-' ||
        LPAD(CAST(nextval('lead_external_seq') AS TEXT), 8, '0');
    RETURN NEW;
END;
`$`$ LANGUAGE plpgsql;

CREATE SEQUENCE IF NOT EXISTS lead_external_seq START 100000;
"@

$v004 = Get-Content (Join-Path $monolith "V004__lead.sql") -Raw -Encoding UTF8

# Remove cross-service FKs; keep intra-lead FKs
$stripPatterns = @(
    '(?m)^ALTER TABLE leads ADD CONSTRAINT fk_leads_tenant.*\r?\n',
    '(?m)^ALTER TABLE lead_assignments ADD CONSTRAINT fk_lead_assignments_user.*\r?\n',
    '(?m)^-- Note: fk_leads_customer deferred.*\r?\n'
)
foreach ($p in $stripPatterns) {
    $v004 = $v004 -replace $p, ''
}

$v004 = $v004 -replace '(?m)^-- V004__lead\.sql.*\r?\n', "-- Lead Service: all lead domain tables (from DDS V004)`n"
$v004 = $v004 -replace '(?m)^-- Lead: Leads.*\r?\n', ''

$v003 = @"
-- Lead Service: indexes (from DDS V004 + V028 subset)
CREATE INDEX IF NOT EXISTS idx_leads_metadata_gin ON leads USING gin (metadata);
CREATE INDEX IF NOT EXISTS idx_leads_cover ON leads(tenant_id, status) INCLUDE (score, assigned_to, created_at);
CREATE INDEX IF NOT EXISTS idx_leads_fts ON leads USING gin(
    to_tsvector('english', customer_name || ' ' || COALESCE(transcript, ''))
);
CREATE INDEX IF NOT EXISTS idx_leads_date_qualified ON leads(tenant_id, (metadata->>'qualified_at')::timestamptz)
    WHERE status = 'QUALIFIED';
"@

[System.IO.File]::WriteAllText((Join-Path $out "V001__foundation.sql"), $v001.Trim() + "`n", [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText((Join-Path $out "V002__lead_tables.sql"), $v004.Trim() + "`n", [System.Text.UTF8Encoding]::new($false))
[System.IO.File]::WriteAllText((Join-Path $out "V003__lead_indexes.sql"), $v003.Trim() + "`n", [System.Text.UTF8Encoding]::new($false))

Write-Host "Lead service split written to $out"
