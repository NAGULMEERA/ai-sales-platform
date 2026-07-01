-- ============================================================================
-- V016__integration.sql
-- Generated from DB.html v7.0: Integration and tenant migration (§20-21)
-- Lab monolith only. Split per service for production (UUID refs, no cross-FK).
-- ============================================================================

-- DB.html 20.1 provider_configs
CREATE TABLE provider_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    provider_type TEXT,
    provider_name TEXT,
    config JSONB DEFAULT '{}'::jsonb,
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 20.2 webhooks
CREATE TABLE webhooks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    url TEXT,
    events TEXT[],
    secret TEXT,
    enabled BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_webhooks_tenant ON webhooks(tenant_id);
CREATE INDEX IF NOT EXISTS idx_webhooks_enabled ON webhooks(enabled);

-- DB.html 20.3 webhook_logs
CREATE TABLE webhook_logs (
    id BIGSERIAL PRIMARY KEY,
    webhook_id UUID,
    event_type TEXT,
    payload JSONB DEFAULT '{}'::jsonb,
    response_status INTEGER,
    response_body TEXT,
    duration_ms INTEGER,
    success BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 20.4 oauth_tokens
CREATE TABLE oauth_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    provider TEXT,
    access_token TEXT,
    refresh_token TEXT,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 20.5 contract_tests
CREATE TABLE contract_tests (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    provider TEXT,
    consumer TEXT,
    test_name TEXT,
    status TEXT,
    last_run_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 21.1 tenant_migration_jobs
CREATE TABLE tenant_migration_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    source_tier TEXT,
    target_tier TEXT,
    status TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tenant_migration_jobs_tenant ON tenant_migration_jobs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_tenant_migration_jobs_status ON tenant_migration_jobs(status);

-- DB.html 21.2 tenant_migration_logs
CREATE TABLE tenant_migration_logs (
    id BIGSERIAL PRIMARY KEY,
    migration_job_id UUID,
    step TEXT,
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tenant_migration_logs_job ON tenant_migration_logs(migration_job_id);
