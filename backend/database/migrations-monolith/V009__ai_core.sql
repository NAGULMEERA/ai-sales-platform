-- ============================================================================
-- V009__ai_core.sql
-- AI Core: Providers, Models, Prompts, LLM Requests, Token Usage
-- ============================================================================

-- ============================================================================
-- ai_providers
-- ============================================================================
CREATE TABLE ai_providers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    provider_key VARCHAR(50) NOT NULL,
    description TEXT,
    default_model VARCHAR(100),
    config_schema JSONB DEFAULT '{}'::jsonb,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

ALTER TABLE ai_providers ADD CONSTRAINT uk_ai_providers_key UNIQUE (provider_key);

-- ============================================================================
-- ai_provider_credentials
-- ============================================================================
CREATE TABLE ai_provider_credentials (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    provider_id UUID NOT NULL,
    credential_name VARCHAR(100) NOT NULL,
    credential_value TEXT NOT NULL,
    is_encrypted BOOLEAN DEFAULT true,
    expires_at TIMESTAMPTZ,
    last_used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_ai_provider_credentials_tenant ON ai_provider_credentials(tenant_id);
CREATE INDEX idx_ai_provider_credentials_provider ON ai_provider_credentials(provider_id);

-- ============================================================================
-- ai_models
-- ============================================================================
CREATE TABLE ai_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider_id UUID NOT NULL,
    model_name VARCHAR(100) NOT NULL,
    model_type VARCHAR(50) NOT NULL,
    description TEXT,
    context_window INTEGER,
    max_tokens INTEGER,
    cost_per_1k_input DECIMAL(19,6),
    cost_per_1k_output DECIMAL(19,6),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_ai_models_provider ON ai_models(provider_id);
CREATE INDEX idx_ai_models_active ON ai_models(is_active);

-- ============================================================================
-- prompt_templates
-- ============================================================================
CREATE TABLE prompt_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    purpose VARCHAR(50) NOT NULL,
    template TEXT NOT NULL,
    variables JSONB DEFAULT '{}'::jsonb,
    version INTEGER NOT NULL DEFAULT 1,
    is_active BOOLEAN DEFAULT true,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_prompt_templates_tenant ON prompt_templates(tenant_id);
CREATE INDEX idx_prompt_templates_purpose ON prompt_templates(purpose);
CREATE INDEX idx_prompt_templates_active ON prompt_templates(tenant_id, is_active);

-- ============================================================================
-- prompt_versions
-- ============================================================================
CREATE TABLE prompt_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prompt_template_id UUID NOT NULL,
    version INTEGER NOT NULL,
    template TEXT NOT NULL,
    variables JSONB DEFAULT '{}'::jsonb,
    changelog TEXT,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prompt_versions_template ON prompt_versions(prompt_template_id, version DESC);

-- ============================================================================
-- prompt_variables
-- ============================================================================
CREATE TABLE prompt_variables (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prompt_template_id UUID NOT NULL,
    variable_name VARCHAR(100) NOT NULL,
    variable_type VARCHAR(50) NOT NULL,
    default_value TEXT,
    description TEXT,
    is_required BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prompt_variables_template ON prompt_variables(prompt_template_id);

-- ============================================================================
-- prompt_execution_logs
-- ============================================================================
CREATE TABLE prompt_execution_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    prompt_template_id UUID,
    prompt_used TEXT NOT NULL,
    llm_provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    input_tokens INTEGER,
    output_tokens INTEGER,
    cost DECIMAL(19,6),
    response_preview TEXT,
    duration_ms INTEGER,
    success BOOLEAN DEFAULT true,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prompt_execution_logs_tenant ON prompt_execution_logs(tenant_id, created_at DESC);
CREATE INDEX idx_prompt_execution_logs_template ON prompt_execution_logs(prompt_template_id);

-- ============================================================================
-- prompt_evaluations
-- ============================================================================
CREATE TABLE prompt_evaluations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    prompt_template_id UUID NOT NULL,
    lead_id UUID,
    accuracy_score DECIMAL(3,2),
    response_quality INTEGER,
    human_feedback TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_prompt_evaluations_template ON prompt_evaluations(prompt_template_id);

-- ============================================================================
-- llm_requests
-- ============================================================================
CREATE TABLE llm_requests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    lead_id UUID,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    prompt TEXT NOT NULL,
    response TEXT,
    input_tokens INTEGER,
    output_tokens INTEGER,
    cost DECIMAL(19,6),
    duration_ms INTEGER,
    success BOOLEAN DEFAULT true,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_llm_requests_tenant ON llm_requests(tenant_id, created_at DESC);
CREATE INDEX idx_llm_requests_lead ON llm_requests(lead_id);
CREATE INDEX idx_llm_requests_provider ON llm_requests(provider);

-- ============================================================================
-- token_usage
-- ============================================================================
CREATE TABLE token_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    lead_id UUID,
    model VARCHAR(100) NOT NULL,
    prompt_type VARCHAR(50) NOT NULL,
    input_tokens INTEGER,
    output_tokens INTEGER,
    total_tokens INTEGER,
    cost DECIMAL(19,6),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_token_usage_tenant ON token_usage(tenant_id, created_at DESC);
CREATE INDEX idx_token_usage_lead ON token_usage(lead_id);

-- ============================================================================
-- ai_cost_tracking
-- ============================================================================
CREATE TABLE ai_cost_tracking (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    date DATE NOT NULL,
    feature VARCHAR(50) NOT NULL,
    model_used VARCHAR(100),
    total_tokens INTEGER,
    total_cost DECIMAL(19,6),
    cache_hits INTEGER DEFAULT 0,
    cache_misses INTEGER DEFAULT 0,
    cost_saved DECIMAL(19,6) DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_cost_tracking_tenant ON ai_cost_tracking(tenant_id, date DESC);

-- ============================================================================
-- ai_evaluations
-- ============================================================================
CREATE TABLE ai_evaluations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    lead_id UUID,
    evaluation_type VARCHAR(50) NOT NULL,
    ai_output JSONB DEFAULT '{}'::jsonb,
    human_feedback JSONB DEFAULT '{}'::jsonb,
    accuracy_score DECIMAL(3,2),
    response_quality INTEGER,
    latency_ms INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_evaluations_tenant ON ai_evaluations(tenant_id, created_at DESC);

-- ============================================================================
-- ai_execution_logs
-- ============================================================================
CREATE TABLE ai_execution_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    lead_id UUID,
    execution_type VARCHAR(50) NOT NULL,
    execution_data JSONB DEFAULT '{}'::jsonb,
    status VARCHAR(50) NOT NULL,
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_ai_execution_logs_tenant ON ai_execution_logs(tenant_id, created_at DESC);

-- ============================================================================
-- model_performance_metrics
-- ============================================================================
CREATE TABLE model_performance_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    model VARCHAR(100) NOT NULL,
    avg_response_time_ms INTEGER,
    success_rate DECIMAL(3,2),
    avg_tokens_per_request INTEGER,
    avg_cost_per_request DECIMAL(19,6),
    total_requests INTEGER,
    period_start DATE,
    period_end DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_model_performance_tenant ON model_performance_metrics(tenant_id, created_at DESC);

-- ============================================================================
-- prompt_drift_detection
-- ============================================================================
CREATE TABLE prompt_drift_detection (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    prompt_template_id UUID NOT NULL,
    old_prompt TEXT NOT NULL,
    new_prompt TEXT NOT NULL,
    embedding_drift_score DECIMAL(3,2),
    performance_drift_score DECIMAL(3,2),
    change_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_prompt_drift_tenant ON prompt_drift_detection(tenant_id);
CREATE INDEX idx_prompt_drift_template ON prompt_drift_detection(prompt_template_id);

-- ============================================================================
-- Foreign Key Constraints
-- ============================================================================
ALTER TABLE ai_provider_credentials ADD CONSTRAINT fk_ai_creds_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE ai_provider_credentials ADD CONSTRAINT fk_ai_creds_provider FOREIGN KEY (provider_id) REFERENCES ai_providers(id);
ALTER TABLE ai_models ADD CONSTRAINT fk_ai_models_provider FOREIGN KEY (provider_id) REFERENCES ai_providers(id);
ALTER TABLE prompt_templates ADD CONSTRAINT fk_prompt_templates_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE prompt_versions ADD CONSTRAINT fk_prompt_versions_template FOREIGN KEY (prompt_template_id) REFERENCES prompt_templates(id) ON DELETE CASCADE;
ALTER TABLE prompt_variables ADD CONSTRAINT fk_prompt_variables_template FOREIGN KEY (prompt_template_id) REFERENCES prompt_templates(id) ON DELETE CASCADE;
ALTER TABLE prompt_execution_logs ADD CONSTRAINT fk_prompt_exec_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE prompt_evaluations ADD CONSTRAINT fk_prompt_evaluations_template FOREIGN KEY (prompt_template_id) REFERENCES prompt_templates(id) ON DELETE CASCADE;
ALTER TABLE llm_requests ADD CONSTRAINT fk_llm_requests_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE token_usage ADD CONSTRAINT fk_token_usage_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE ai_cost_tracking ADD CONSTRAINT fk_ai_cost_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE ai_evaluations ADD CONSTRAINT fk_ai_evaluations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE ai_execution_logs ADD CONSTRAINT fk_ai_execution_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE prompt_drift_detection ADD CONSTRAINT fk_prompt_drift_template FOREIGN KEY (prompt_template_id) REFERENCES prompt_templates(id) ON DELETE CASCADE;
