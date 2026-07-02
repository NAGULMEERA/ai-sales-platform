-- EPIC-02: extend tenants table for full tenant lifecycle management.

ALTER TABLE tenants RENAME COLUMN plan TO subscription_plan;

ALTER TABLE tenants ADD COLUMN tenant_code VARCHAR(50);
ALTER TABLE tenants ADD COLUMN industry VARCHAR(50);
ALTER TABLE tenants ADD COLUMN timezone VARCHAR(64) NOT NULL DEFAULT 'UTC';
ALTER TABLE tenants ADD COLUMN language VARCHAR(10) NOT NULL DEFAULT 'en';
ALTER TABLE tenants ADD COLUMN logo_url VARCHAR(512);
ALTER TABLE tenants ADD COLUMN deleted BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE tenants ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

UPDATE tenants
SET tenant_code = UPPER(REPLACE(slug, '-', '_'))
WHERE tenant_code IS NULL;

UPDATE tenants
SET industry = 'REAL_ESTATE'
WHERE industry IS NULL;

UPDATE tenants
SET deleted = TRUE,
    status = 'SUSPENDED'
WHERE status = 'DELETED';

ALTER TABLE tenants ALTER COLUMN tenant_code SET NOT NULL;

CREATE UNIQUE INDEX uk_tenants_tenant_code ON tenants(tenant_code);
CREATE INDEX idx_tenants_industry ON tenants(industry);
CREATE INDEX idx_tenants_subscription_plan ON tenants(subscription_plan);
CREATE INDEX idx_tenants_active ON tenants(status, deleted) WHERE deleted = FALSE;
