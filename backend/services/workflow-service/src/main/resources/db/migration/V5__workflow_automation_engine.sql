-- Lightweight trigger → condition → action automation (no BPM engine).

CREATE TABLE IF NOT EXISTS workflow_rule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    organization_id UUID,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    trigger_type VARCHAR(60) NOT NULL,
    conditions JSONB NOT NULL DEFAULT '[]'::jsonb,
    actions JSONB NOT NULL DEFAULT '[]'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    max_retries INT NOT NULL DEFAULT 3,
    retry_backoff_seconds INT NOT NULL DEFAULT 30,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_workflow_rule_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX IF NOT EXISTS idx_workflow_rule_tenant_trigger
    ON workflow_rule (tenant_id, trigger_type)
    WHERE deleted_at IS NULL AND enabled = TRUE;

CREATE TABLE IF NOT EXISTS workflow_automation_execution (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    organization_id UUID,
    rule_id UUID NOT NULL REFERENCES workflow_rule (id),
    rule_code VARCHAR(100) NOT NULL,
    trigger_type VARCHAR(60) NOT NULL,
    business_key VARCHAR(255) NOT NULL,
    state VARCHAR(40) NOT NULL DEFAULT 'RUNNING',
    retry_count INT NOT NULL DEFAULT 0,
    last_error VARCHAR(2000),
    correlation_id VARCHAR(255),
    context JSONB NOT NULL DEFAULT '{}'::jsonb,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    next_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_workflow_automation_state CHECK (
        state IN ('RUNNING', 'COMPLETED', 'FAILED', 'RETRYING')
    )
);

CREATE INDEX IF NOT EXISTS idx_workflow_automation_execution_tenant_state
    ON workflow_automation_execution (tenant_id, state);

CREATE INDEX IF NOT EXISTS idx_workflow_automation_execution_retry
    ON workflow_automation_execution (next_retry_at)
    WHERE state = 'RETRYING';

CREATE TABLE IF NOT EXISTS workflow_execution_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    execution_id UUID NOT NULL REFERENCES workflow_automation_execution (id),
    step_type VARCHAR(40) NOT NULL,
    step_name VARCHAR(120) NOT NULL,
    status VARCHAR(40) NOT NULL,
    detail VARCHAR(2000),
    payload JSONB NOT NULL DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_workflow_execution_history_execution
    ON workflow_execution_history (tenant_id, execution_id, occurred_at ASC);
