-- Platform customer core. Keep Party rewrite / industry profiles out of Phase 1.

CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    organization_id UUID,
    full_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50),
    email VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'PROSPECT',
    source_type VARCHAR(40) NOT NULL DEFAULT 'MANUAL',
    source_lead_id UUID,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_customers_status CHECK (status IN ('PROSPECT', 'ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT ck_customers_source CHECK (source_type IN ('LEAD_CONVERSION', 'IMPORT', 'MANUAL')),
    CONSTRAINT ck_customers_contact CHECK (
        (phone IS NOT NULL AND btrim(phone) <> '')
        OR (email IS NOT NULL AND btrim(email) <> '')
    )
);

CREATE INDEX idx_customers_tenant_status ON customers (tenant_id, status)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_customers_tenant_phone ON customers (tenant_id, phone)
    WHERE deleted_at IS NULL AND phone IS NOT NULL;
CREATE INDEX idx_customers_tenant_email ON customers (tenant_id, email)
    WHERE deleted_at IS NULL AND email IS NOT NULL;
CREATE INDEX idx_customers_tenant_lead ON customers (tenant_id, source_lead_id)
    WHERE source_lead_id IS NOT NULL AND deleted_at IS NULL;

CREATE TRIGGER trg_customers_updated_at
    BEFORE UPDATE ON customers
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TABLE customer_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers (id),
    address_type VARCHAR(50) NOT NULL DEFAULT 'HOME',
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100),
    postal_code VARCHAR(20),
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ
);

CREATE INDEX idx_customer_addresses_customer ON customer_addresses (tenant_id, customer_id)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_customer_addresses_updated_at
    BEFORE UPDATE ON customer_addresses
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
