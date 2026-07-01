-- ============================================================================
-- V023__scheduler.sql
-- Scheduled jobs (synthesized — no dedicated DB.html section)
-- ============================================================================

CREATE TABLE IF NOT EXISTS scheduled_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID,
    job_name VARCHAR(255) NOT NULL,
    job_type VARCHAR(100) NOT NULL,
    cron_expression VARCHAR(100) NOT NULL,
    payload JSONB DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT true,
    last_run_at TIMESTAMPTZ,
    next_run_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_scheduled_jobs_tenant ON scheduled_jobs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_scheduled_jobs_next_run ON scheduled_jobs(next_run_at) WHERE enabled = true;

CREATE TABLE IF NOT EXISTS job_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'RUNNING',
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    result JSONB DEFAULT '{}'::jsonb,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_job_executions_job ON job_executions(job_id, started_at DESC);
CREATE INDEX IF NOT EXISTS idx_job_executions_status ON job_executions(status);

ALTER TABLE job_executions DROP CONSTRAINT IF EXISTS fk_job_executions_job;
ALTER TABLE job_executions ADD CONSTRAINT fk_job_executions_job
    FOREIGN KEY (job_id) REFERENCES scheduled_jobs(id) ON DELETE CASCADE;
