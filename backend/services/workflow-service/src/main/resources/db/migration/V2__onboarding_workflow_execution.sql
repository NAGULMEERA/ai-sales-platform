-- Orchestration state for platform workflows (execution only — no business rules).

ALTER TABLE workflow_execution
    ADD COLUMN IF NOT EXISTS definition_key VARCHAR(100) NOT NULL DEFAULT 'ONBOARDING_V1';

ALTER TABLE workflow_execution
    ADD COLUMN IF NOT EXISTS business_key VARCHAR(255) NOT NULL DEFAULT '';

ALTER TABLE workflow_execution
    ADD COLUMN IF NOT EXISTS state VARCHAR(64) NOT NULL DEFAULT 'CREATED';

ALTER TABLE workflow_execution
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(255);

ALTER TABLE workflow_execution
    ADD COLUMN IF NOT EXISTS completed_at TIMESTAMPTZ;

-- Replace empty default business keys before unique constraint (fresh installs only have shell rows).
UPDATE workflow_execution SET business_key = id::text WHERE business_key = '';

ALTER TABLE workflow_execution
    ALTER COLUMN business_key DROP DEFAULT;

ALTER TABLE workflow_execution
    ALTER COLUMN definition_key DROP DEFAULT;

CREATE UNIQUE INDEX IF NOT EXISTS uq_workflow_execution_tenant_def_business
    ON workflow_execution (tenant_id, definition_key, business_key);

CREATE INDEX IF NOT EXISTS idx_workflow_execution_state
    ON workflow_execution (tenant_id, state);
