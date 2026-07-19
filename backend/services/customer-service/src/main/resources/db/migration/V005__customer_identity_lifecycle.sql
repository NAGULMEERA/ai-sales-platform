-- Identity resolution, contact methods, preferences, consent, timeline, duplicates, idempotency.

ALTER TABLE customers
    ADD COLUMN IF NOT EXISTS customer_number VARCHAR(64),
    ADD COLUMN IF NOT EXISTS whatsapp VARCHAR(50),
    ADD COLUMN IF NOT EXISTS external_crm_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS government_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS gender VARCHAR(30),
    ADD COLUMN IF NOT EXISTS date_of_birth DATE,
    ADD COLUMN IF NOT EXISTS language VARCHAR(20),
    ADD COLUMN IF NOT EXISTS preferred_channel VARCHAR(50),
    ADD COLUMN IF NOT EXISTS preferences JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS merged_into_customer_id UUID;

CREATE UNIQUE INDEX IF NOT EXISTS uq_customers_tenant_customer_number
    ON customers (tenant_id, customer_number)
    WHERE customer_number IS NOT NULL AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_customers_tenant_whatsapp
    ON customers (tenant_id, whatsapp)
    WHERE deleted_at IS NULL AND whatsapp IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_customers_tenant_external_crm
    ON customers (tenant_id, external_crm_id)
    WHERE deleted_at IS NULL AND external_crm_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_customers_tenant_government_id
    ON customers (tenant_id, government_id)
    WHERE deleted_at IS NULL AND government_id IS NOT NULL;

CREATE OR REPLACE FUNCTION generate_customer_number()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.customer_number IS NULL OR btrim(NEW.customer_number) = '' THEN
        NEW.customer_number := 'CU-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-' ||
            UPPER(SUBSTRING(REPLACE(NEW.id::text, '-', '') FROM 1 FOR 8));
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_customers_customer_number ON customers;
CREATE TRIGGER trg_customers_customer_number
    BEFORE INSERT ON customers
    FOR EACH ROW EXECUTE FUNCTION generate_customer_number();

CREATE TABLE IF NOT EXISTS customer_contact_methods (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers (id),
    method_type VARCHAR(30) NOT NULL,
    value VARCHAR(255) NOT NULL,
    label VARCHAR(100),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMPTZ,
    verified_by UUID,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_contact_method_type CHECK (
        method_type IN ('PHONE', 'EMAIL', 'WHATSAPP', 'SOCIAL')
    )
);

CREATE INDEX IF NOT EXISTS idx_customer_contacts_customer
    ON customer_contact_methods (tenant_id, customer_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_customer_contacts_value
    ON customer_contact_methods (tenant_id, method_type, value)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_customer_contact_methods_updated_at
    BEFORE UPDATE ON customer_contact_methods
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TABLE IF NOT EXISTS customer_consents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers (id),
    consent_type VARCHAR(100) NOT NULL,
    consent_version VARCHAR(40) NOT NULL DEFAULT '1',
    granted BOOLEAN NOT NULL DEFAULT TRUE,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    withdrawn_at TIMESTAMPTZ,
    source VARCHAR(50),
    created_by UUID,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    CONSTRAINT uq_customer_consent UNIQUE (tenant_id, customer_id, consent_type, consent_version)
);

CREATE INDEX IF NOT EXISTS idx_customer_consents_customer
    ON customer_consents (tenant_id, customer_id);

CREATE TABLE IF NOT EXISTS customer_timeline (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers (id),
    event_type VARCHAR(80) NOT NULL,
    summary TEXT,
    event_data JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID
);

CREATE INDEX IF NOT EXISTS idx_customer_timeline_customer
    ON customer_timeline (tenant_id, customer_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS customer_interactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers (id),
    interaction_type VARCHAR(80) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    summary TEXT,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID
);

CREATE INDEX IF NOT EXISTS idx_customer_interactions_customer
    ON customer_interactions (tenant_id, customer_id, occurred_at DESC);

CREATE TABLE IF NOT EXISTS customer_duplicates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL REFERENCES customers (id),
    duplicate_of_customer_id UUID NOT NULL REFERENCES customers (id),
    match_reasons TEXT NOT NULL DEFAULT '',
    similarity_score NUMERIC(5, 4) NOT NULL DEFAULT 1.0,
    match_strength VARCHAR(20) NOT NULL DEFAULT 'PROBABLE',
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved BOOLEAN NOT NULL DEFAULT FALSE,
    resolved_at TIMESTAMPTZ,
    merged_into_customer_id UUID,
    CONSTRAINT ck_customer_dup_strength CHECK (match_strength IN ('EXACT', 'PROBABLE'))
);

CREATE INDEX IF NOT EXISTS idx_customer_duplicates_open
    ON customer_duplicates (tenant_id, customer_id)
    WHERE resolved = FALSE;

CREATE TABLE IF NOT EXISTS idempotency_keys (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    operation VARCHAR(100) NOT NULL,
    resource_id UUID,
    response_body TEXT NOT NULL,
    http_status INT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_idempotency_keys_expires ON idempotency_keys (expires_at);
