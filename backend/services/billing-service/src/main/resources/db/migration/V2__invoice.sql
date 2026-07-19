-- Billing owns invoices. AI usage amounts are snapshotted at create/issue time.

CREATE TABLE invoice (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    organization_id UUID,
    period_start TIMESTAMPTZ NOT NULL,
    period_end TIMESTAMPTZ NOT NULL,
    status VARCHAR(30) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    source VARCHAR(50) NOT NULL,
    subtotal_usd NUMERIC(19, 8) NOT NULL DEFAULT 0,
    total_usd NUMERIC(19, 8) NOT NULL DEFAULT 0,
    issued_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    deleted_by VARCHAR(100),
    CONSTRAINT uq_invoice_tenant_period_source UNIQUE (tenant_id, period_start, period_end, source),
    CONSTRAINT chk_invoice_period CHECK (period_end > period_start)
);

CREATE INDEX idx_invoice_tenant_status ON invoice (tenant_id, status) WHERE deleted_at IS NULL;

CREATE TABLE invoice_line_item (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    invoice_id UUID NOT NULL REFERENCES invoice (id),
    line_code VARCHAR(100) NOT NULL,
    description VARCHAR(500) NOT NULL,
    quantity BIGINT NOT NULL DEFAULT 0,
    unit_amount_usd NUMERIC(19, 8) NOT NULL DEFAULT 0,
    line_total_usd NUMERIC(19, 8) NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invoice_line_item_invoice ON invoice_line_item (invoice_id);

COMMENT ON TABLE invoice IS 'Billing invoice aggregate; AI_USAGE source snapshots AI ledger estimates';
