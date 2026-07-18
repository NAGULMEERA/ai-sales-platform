-- Opportunity + Quote aggregates. Lead assignment pool stays in lead-service.
-- Scaffold `deal` table from V1 remains unused; Opportunity is the aggregate root.

CREATE TABLE opportunity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    organization_id UUID,
    lead_id UUID,
    customer_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    amount NUMERIC(19, 4),
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    probability INT,
    expected_close_date DATE,
    assigned_to UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_opportunity_status CHECK (
        status IN ('OPEN', 'QUOTED', 'WON', 'LOST', 'CANCELLED')
    ),
    CONSTRAINT ck_opportunity_probability CHECK (
        probability IS NULL OR (probability >= 0 AND probability <= 100)
    ),
    CONSTRAINT ck_opportunity_amount CHECK (amount IS NULL OR amount >= 0)
);

CREATE INDEX idx_opportunity_tenant_status ON opportunity (tenant_id, status)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_opportunity_tenant_customer ON opportunity (tenant_id, customer_id)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_opportunity_tenant_lead ON opportunity (tenant_id, lead_id)
    WHERE lead_id IS NOT NULL AND deleted_at IS NULL;
CREATE INDEX idx_opportunity_tenant_assigned ON opportunity (tenant_id, assigned_to)
    WHERE assigned_to IS NOT NULL AND deleted_at IS NULL;

CREATE TRIGGER trg_opportunity_updated_at
    BEFORE UPDATE ON opportunity
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TABLE quote (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    opportunity_id UUID NOT NULL REFERENCES opportunity (id),
    quote_version INT NOT NULL DEFAULT 1,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    currency VARCHAR(3) NOT NULL DEFAULT 'INR',
    total_amount NUMERIC(19, 4) NOT NULL DEFAULT 0,
    valid_until DATE,
    notes VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_quote_status CHECK (
        status IN ('DRAFT', 'SENT', 'ACCEPTED', 'REJECTED', 'SUPERSEDED')
    ),
    CONSTRAINT ck_quote_total CHECK (total_amount >= 0),
    CONSTRAINT uq_quote_opportunity_version UNIQUE (tenant_id, opportunity_id, quote_version)
);

CREATE INDEX idx_quote_tenant_opportunity ON quote (tenant_id, opportunity_id)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_quote_updated_at
    BEFORE UPDATE ON quote
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TABLE quote_line_item (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    quote_id UUID NOT NULL REFERENCES quote (id),
    product_id UUID,
    offer_id UUID,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    quantity NUMERIC(19, 4) NOT NULL,
    unit_price NUMERIC(19, 4) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    line_total NUMERIC(19, 4) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_quote_line_quantity CHECK (quantity > 0),
    CONSTRAINT ck_quote_line_unit_price CHECK (unit_price >= 0),
    CONSTRAINT ck_quote_line_total CHECK (line_total >= 0),
    CONSTRAINT ck_quote_line_catalog_ref CHECK (product_id IS NOT NULL OR offer_id IS NOT NULL)
);

CREATE INDEX idx_quote_line_quote ON quote_line_item (tenant_id, quote_id);
