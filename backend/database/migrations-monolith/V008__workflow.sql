-- ============================================================================
-- V008__workflow.sql
-- Workflow: Workflows, Steps, Transitions, Executions, Saga
-- ============================================================================

-- ============================================================================
-- workflows
-- ============================================================================
CREATE TABLE workflows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    industry VARCHAR(50) NOT NULL,
    steps JSONB NOT NULL,
    transitions JSONB DEFAULT '{}'::jsonb,
    definition_version INTEGER NOT NULL DEFAULT 1,
    status workflow_status NOT NULL DEFAULT 'DRAFT',
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ NULL,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_workflows_tenant ON workflows(tenant_id);
CREATE INDEX idx_workflows_status ON workflows(status);
CREATE INDEX idx_workflows_industry ON workflows(industry);
CREATE INDEX idx_workflows_active ON workflows(tenant_id, status) WHERE status = 'PUBLISHED' AND deleted_at IS NULL;

-- ============================================================================
-- workflow_versions
-- ============================================================================
CREATE TABLE workflow_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL,
    version INTEGER NOT NULL,
    steps JSONB NOT NULL,
    transitions JSONB DEFAULT '{}'::jsonb,
    status workflow_status NOT NULL DEFAULT 'DRAFT',
    changelog TEXT,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_versions_workflow ON workflow_versions(workflow_id, version DESC);

-- ============================================================================
-- workflow_steps
-- ============================================================================
CREATE TABLE workflow_steps (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL,
    step_order INTEGER NOT NULL,
    step_key VARCHAR(100) NOT NULL,
    step_type VARCHAR(50) NOT NULL,
    config JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_steps_workflow ON workflow_steps(workflow_id, step_order);

-- ============================================================================
-- workflow_transitions
-- ============================================================================
CREATE TABLE workflow_transitions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL,
    from_step VARCHAR(100) NOT NULL,
    to_step VARCHAR(100) NOT NULL,
    condition VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_transitions_workflow ON workflow_transitions(workflow_id);

-- ============================================================================
-- workflow_instances
-- ============================================================================
CREATE TABLE workflow_instances (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_id UUID NOT NULL,
    lead_id UUID NOT NULL,
    current_step VARCHAR(100) NOT NULL,
    step_data JSONB DEFAULT '{}'::jsonb,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_PROGRESS',
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    created_by UUID,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_workflow_instances_workflow ON workflow_instances(workflow_id);
CREATE INDEX idx_workflow_instances_lead ON workflow_instances(lead_id);
CREATE INDEX idx_workflow_instances_status ON workflow_instances(status);

-- ============================================================================
-- workflow_executions
-- ============================================================================
CREATE TABLE workflow_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_instance_id UUID NOT NULL,
    step_key VARCHAR(100) NOT NULL,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    execution_data JSONB DEFAULT '{}'::jsonb,
    error_message TEXT,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_workflow_executions_instance ON workflow_executions(workflow_instance_id, started_at DESC);

-- ============================================================================
-- workflow_events
-- ============================================================================
CREATE TABLE workflow_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_instance_id UUID NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    event_data JSONB DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_events_instance ON workflow_events(workflow_instance_id, occurred_at DESC);

-- ============================================================================
-- workflow_variables
-- ============================================================================
CREATE TABLE workflow_variables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_instance_id UUID NOT NULL,
    variable_key VARCHAR(100) NOT NULL,
    variable_value JSONB NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_variables_instance ON workflow_variables(workflow_instance_id, variable_key);

-- ============================================================================
-- workflow_error_logs
-- ============================================================================
CREATE TABLE workflow_error_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_execution_id UUID NOT NULL,
    error_code VARCHAR(100),
    error_message TEXT,
    error_stack TEXT,
    recovered BOOLEAN DEFAULT false,
    recovery_action TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_error_logs_execution ON workflow_error_logs(workflow_execution_id);

-- ============================================================================
-- workflow_snapshots
-- ============================================================================
CREATE TABLE workflow_snapshots (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workflow_instance_id UUID NOT NULL,
    snapshot_data JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_workflow_snapshots_instance ON workflow_snapshots(workflow_instance_id);

-- ============================================================================
-- Foreign Key Constraints
-- ============================================================================
ALTER TABLE workflows ADD CONSTRAINT fk_workflows_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE workflow_versions ADD CONSTRAINT fk_workflow_versions_workflow FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE;
ALTER TABLE workflow_steps ADD CONSTRAINT fk_workflow_steps_workflow FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE;
ALTER TABLE workflow_transitions ADD CONSTRAINT fk_workflow_transitions_workflow FOREIGN KEY (workflow_id) REFERENCES workflows(id) ON DELETE CASCADE;
ALTER TABLE workflow_instances ADD CONSTRAINT fk_workflow_instances_workflow FOREIGN KEY (workflow_id) REFERENCES workflows(id);
ALTER TABLE workflow_instances ADD CONSTRAINT fk_workflow_instances_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;
ALTER TABLE workflow_executions ADD CONSTRAINT fk_workflow_executions_instance FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE;
ALTER TABLE workflow_events ADD CONSTRAINT fk_workflow_events_instance FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE;
ALTER TABLE workflow_variables ADD CONSTRAINT fk_workflow_variables_instance FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE;
ALTER TABLE workflow_error_logs ADD CONSTRAINT fk_workflow_error_logs_execution FOREIGN KEY (workflow_execution_id) REFERENCES workflow_executions(id) ON DELETE CASCADE;
ALTER TABLE workflow_snapshots ADD CONSTRAINT fk_workflow_snapshots_instance FOREIGN KEY (workflow_instance_id) REFERENCES workflow_instances(id) ON DELETE CASCADE;

-- Add triggers
CREATE TRIGGER trg_workflows_updated_at BEFORE UPDATE ON workflows
FOR EACH ROW EXECUTE FUNCTION update_updated_at();
