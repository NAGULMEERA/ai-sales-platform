-- Payment collection for issued invoices. Provider is pluggable (STUB | STRIPE).

ALTER TABLE invoice
    ADD COLUMN paid_at TIMESTAMPTZ;

CREATE TABLE payment (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    invoice_id UUID NOT NULL REFERENCES invoice (id),
    status VARCHAR(30) NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_payment_id VARCHAR(255),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    amount_usd NUMERIC(19, 8) NOT NULL,
    client_secret VARCHAR(500),
    failure_message VARCHAR(1000),
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_payment_tenant_invoice ON payment (tenant_id, invoice_id);
CREATE UNIQUE INDEX uq_payment_succeeded_invoice
    ON payment (invoice_id)
    WHERE status = 'SUCCEEDED';

COMMENT ON TABLE payment IS 'Invoice payment attempts; STUB or STRIPE via aisales.billing.payment.provider';
