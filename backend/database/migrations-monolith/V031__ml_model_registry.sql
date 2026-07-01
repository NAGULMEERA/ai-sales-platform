-- ============================================================================
-- V031__ml_model_registry.sql
-- Generated from DB.html v7.0: AI/ML model registry and MLOps (§22)
-- Lab monolith only. Split per service for production (UUID refs, no cross-FK).
-- ============================================================================

-- DB.html 22.1 model_registry
CREATE TABLE model_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    model_name TEXT,
    model_provider TEXT,
    model_type TEXT,
    version TEXT,
    metadata JSONB DEFAULT '{}'::jsonb,
    status TEXT,
    deployed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_model_registry_tenant ON model_registry(tenant_id);
CREATE INDEX IF NOT EXISTS idx_model_registry_status ON model_registry(status);
CREATE INDEX IF NOT EXISTS idx_model_registry_provider ON model_registry(model_provider);

-- DB.html 22.2 model_versions
CREATE TABLE model_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_registry_id UUID,
    version_number INTEGER,
    weights_url TEXT,
    metrics JSONB DEFAULT '{}'::jsonb,
    changelog TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_model_versions_registry ON model_versions(model_registry_id, version_number DESC);

-- DB.html 22.3 model_deployments
CREATE TABLE model_deployments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_version_id UUID,
    environment TEXT,
    deployment_config JSONB DEFAULT '{}'::jsonb,
    rollout_percentage INTEGER,
    deployed_at TIMESTAMPTZ,
    undeployed_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_model_deployments_version ON model_deployments(model_version_id);
CREATE INDEX IF NOT EXISTS idx_model_deployments_env ON model_deployments(environment);

-- DB.html 22.4 experiments
CREATE TABLE experiments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    experiment_name TEXT,
    experiment_type TEXT,
    status TEXT,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_experiments_tenant ON experiments(tenant_id);
CREATE INDEX IF NOT EXISTS idx_experiments_status ON experiments(status);

-- DB.html 22.5 experiment_variants
CREATE TABLE experiment_variants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    experiment_id UUID,
    variant_name TEXT,
    variant_config JSONB DEFAULT '{}'::jsonb,
    allocation_percentage INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_experiment_variants_exp ON experiment_variants(experiment_id);

-- DB.html 22.6 experiment_results
CREATE TABLE experiment_results (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    experiment_variant_id UUID,
    metric_name TEXT,
    metric_value DECIMAL(19,4),
    sample_size INTEGER,
    confidence_interval_lower DECIMAL(19,4),
    confidence_interval_upper DECIMAL(19,4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_experiment_results_variant ON experiment_results(experiment_variant_id);

-- DB.html 22.7 fine_tuning_jobs
CREATE TABLE fine_tuning_jobs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    base_model_id UUID,
    fine_tuned_model_id UUID,
    dataset_id UUID,
    status TEXT,
    job_config JSONB DEFAULT '{}'::jsonb,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_fine_tuning_jobs_tenant ON fine_tuning_jobs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_fine_tuning_jobs_status ON fine_tuning_jobs(status);
