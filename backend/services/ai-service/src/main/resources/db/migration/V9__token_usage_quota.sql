-- Track B: durable LLM token ledger + optional per-tenant daily quota override.

CREATE TABLE token_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    organization_id UUID,
    execution_id UUID NOT NULL,
    prompt_code VARCHAR(100) NOT NULL,
    provider VARCHAR(50) NOT NULL,
    model VARCHAR(100) NOT NULL,
    operation VARCHAR(50) NOT NULL DEFAULT 'EXECUTE',
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    business_reference VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_token_usage_execution UNIQUE (execution_id),
    CONSTRAINT ck_token_usage_non_negative CHECK (
        prompt_tokens >= 0 AND completion_tokens >= 0 AND total_tokens >= 0
    )
);

CREATE INDEX idx_token_usage_tenant_day ON token_usage (tenant_id, created_at);
CREATE INDEX idx_token_usage_tenant_prompt ON token_usage (tenant_id, prompt_code);

CREATE TABLE tenant_ai_quota (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    organization_id UUID,
    daily_token_limit BIGINT NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_tenant_ai_quota_tenant UNIQUE (tenant_id),
    CONSTRAINT ck_tenant_ai_quota_positive CHECK (daily_token_limit > 0)
);

CREATE INDEX idx_tenant_ai_quota_enabled ON tenant_ai_quota (tenant_id) WHERE enabled = TRUE;
