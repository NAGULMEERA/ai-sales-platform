-- Tenant assignee pool for ROUND_ROBIN assignment strategy.

CREATE TABLE lead_assignment_pool (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT true,
    last_assigned_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_lead_assignment_pool_tenant_user UNIQUE (tenant_id, user_id)
);

CREATE INDEX idx_lead_assignment_pool_tenant_enabled
    ON lead_assignment_pool(tenant_id, enabled)
    WHERE enabled = true;
