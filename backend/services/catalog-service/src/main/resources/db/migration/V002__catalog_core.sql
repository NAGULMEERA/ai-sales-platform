-- Platform catalog core. Industry specifics (property, vehicle, course) stay in plugins
-- via attributes JSONB / separate plugin schemas — never as first-class Platform Core tables.

CREATE TABLE catalog_product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    organization_id UUID,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    product_type VARCHAR(30) NOT NULL,
    category VARCHAR(100),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    attributes JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_catalog_product_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT ck_catalog_product_type CHECK (product_type IN ('PRODUCT', 'SERVICE')),
    CONSTRAINT ck_catalog_product_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'))
);

CREATE INDEX idx_catalog_product_tenant_status ON catalog_product (tenant_id, status)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_catalog_product_tenant_category ON catalog_product (tenant_id, category)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_catalog_product_tenant_type ON catalog_product (tenant_id, product_type)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_catalog_product_updated_at
    BEFORE UPDATE ON catalog_product
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TABLE catalog_offer (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL REFERENCES catalog_product (id),
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    unit_price NUMERIC(19, 4) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    valid_from TIMESTAMPTZ,
    valid_to TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uk_catalog_offer_tenant_code UNIQUE (tenant_id, code),
    CONSTRAINT ck_catalog_offer_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_catalog_offer_price CHECK (unit_price >= 0)
);

CREATE INDEX idx_catalog_offer_tenant_product ON catalog_offer (tenant_id, product_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_catalog_offer_tenant_status ON catalog_offer (tenant_id, status)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_catalog_offer_updated_at
    BEFORE UPDATE ON catalog_offer
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
