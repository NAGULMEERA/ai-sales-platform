# Apply Chief Architect approved fixes to extracted monolith migrations
param(
    [string]$MigrationDir = "backend/database/migrations-monolith"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
$dir = Join-Path $root $MigrationDir

function Read-Migration($name) {
    Get-Content -Path (Join-Path $dir $name) -Raw -Encoding UTF8
}

function Write-Migration($name, $content) {
    $path = Join-Path $dir $name
    [System.IO.File]::WriteAllText($path, $content.TrimEnd() + "`n", [System.Text.UTF8Encoding]::new($false))
    Write-Host "Fixed: $name"
}

# V002: Remove tenant FKs (tenants table created in V003)
$v002 = Read-Migration "V002__security.sql"
$v002 = $v002 -replace '(?m)^ALTER TABLE users ADD CONSTRAINT fk_users_tenant.*\r?\n', ''
$v002 = $v002 -replace '(?m)^ALTER TABLE roles ADD CONSTRAINT fk_roles_tenant.*\r?\n', ''
$v002 = $v002 -replace '(?m)^ALTER TABLE api_keys ADD CONSTRAINT fk_api_keys_tenant.*\r?\n', ''
if ($v002 -notmatch 'tenant FKs deferred to V003') {
    $v002 = $v002 -replace '(?m)^-- Foreign Key Constraints\s*$', "-- Foreign Key Constraints`n-- Note: tenant FKs deferred to V003 (tenants table created there)"
}
Write-Migration "V002__security.sql" $v002

# V003: Add deferred tenant FKs from V002 after tenants table exists
$v003 = Read-Migration "V003__tenant.sql"
if ($v003 -notmatch 'fk_users_tenant') {
    $deferredFks = @"

-- Deferred tenant FKs from V002 (tenants must exist first)
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE roles ADD CONSTRAINT fk_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE api_keys ADD CONSTRAINT fk_api_keys_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

"@
    $v003 = $v003 -replace '(?m)^-- Add triggers\s*$', ($deferredFks + "-- Add triggers")
}
Write-Migration "V003__tenant.sql" $v003

# V004: Add assigned_to column; remove customer FK (customers in V005)
$v004 = Read-Migration "V004__lead.sql"
if ($v004 -notmatch 'assigned_to UUID') {
    $v004 = $v004 -replace '(?m)(status lead_status NOT NULL DEFAULT ''NEW'',)', "`$1`n    assigned_to UUID,"
}
$v004 = $v004 -replace '(?m)^ALTER TABLE leads ADD CONSTRAINT fk_leads_customer.*\r?\n', ''
if ($v004 -notmatch 'customer FK deferred to V005') {
    $v004 = $v004 -replace '(?m)^-- Foreign Key Constraints\s*$', "-- Foreign Key Constraints`n-- Note: fk_leads_customer deferred to V005 (customers table created there)"
}
Write-Migration "V004__lead.sql" $v004

# V005: Add deferred customer FK to leads
$v005 = Read-Migration "V005__customer.sql"
if ($v005 -notmatch 'fk_leads_customer') {
    $leadFk = @"

-- Deferred customer FK from V004
ALTER TABLE leads ADD CONSTRAINT fk_leads_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;

"@
    $v005 = $v005 -replace '(?m)^-- Add triggers\s*$', ($leadFk + "-- Add triggers")
}
Write-Migration "V005__customer.sql" $v005

# V028: Remove duplicate partial indexes (already in V004/V005)
$v028 = Read-Migration "V028__advanced_indexes.sql"
$v028 = $v028 -replace '(?m)^CREATE INDEX idx_leads_active ON leads.*\r?\n', ''
$v028 = $v028 -replace '(?m)^CREATE INDEX idx_customers_active ON customers.*\r?\n', ''
# Fix invalid leads.data reference (column is metadata, not data)
$v028 = $v028 -replace "data->>'qualified_at'", "metadata->>'qualified_at'"
Write-Migration "V028__advanced_indexes.sql" $v028

Write-Host ""
Write-Host "Monolith migration fixes applied."
