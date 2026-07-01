-- V001__create_lead_table.sql
-- Production Flyway Migration Template

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE lead (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,

    name VARCHAR(150) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(30) NOT NULL,

    status VARCHAR(50) NOT NULL,
    score INTEGER DEFAULT 0,

    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL,

    version BIGINT NOT NULL DEFAULT 0,

    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMPTZ,

    CONSTRAINT chk_lead_score
        CHECK (score >= 0)
);

CREATE INDEX idx_lead_tenant
    ON lead (tenant_id);

CREATE INDEX idx_lead_status
    ON lead (status);

CREATE INDEX idx_lead_created_at
    ON lead (created_at DESC);

CREATE UNIQUE INDEX uq_lead_tenant_email
    ON lead (tenant_id, email);

COMMENT ON TABLE lead IS
'Lead aggregate root';

COMMENT ON COLUMN lead.version IS
'Optimistic locking column';

-- Roll forward only.
-- Never modify an executed Flyway migration.
