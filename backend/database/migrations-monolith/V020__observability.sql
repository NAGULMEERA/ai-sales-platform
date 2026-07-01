-- ============================================================================
-- V020__observability.sql
-- Generated from DB.html v7.0: AI observability (§26)
-- Lab monolith only. Split per service for production (UUID refs, no cross-FK).
-- ============================================================================

-- DB.html 26.1 ai_drift_detection
CREATE TABLE ai_drift_detection (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    model_registry_id UUID,
    drift_type TEXT,
    baseline_embedding vector(1536),
    current_embedding vector(1536),
    drift_score DECIMAL(3,2),
    threshold DECIMAL(3,2),
    alert_triggered BOOLEAN DEFAULT false,
    detected_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_ai_drift_tenant ON ai_drift_detection(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_drift_model ON ai_drift_detection(model_registry_id);
CREATE INDEX IF NOT EXISTS idx_ai_drift_type ON ai_drift_detection(drift_type);

-- DB.html 26.2 ai_cost_metrics
CREATE TABLE ai_cost_metrics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    date DATE,
    feature TEXT,
    model_used TEXT,
    total_tokens INTEGER,
    total_cost DECIMAL(19,4),
    cache_hits INTEGER,
    cache_misses INTEGER,
    cost_saved DECIMAL(19,4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_cost_tenant ON ai_cost_metrics(tenant_id, date DESC);
CREATE INDEX IF NOT EXISTS idx_ai_cost_feature ON ai_cost_metrics(feature);

-- DB.html 26.3 ai_quality_metrics
CREATE TABLE ai_quality_metrics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    date DATE,
    feature TEXT,
    accuracy DECIMAL(3,2),
    hallucination_rate DECIMAL(3,2),
    response_quality DECIMAL(3,2),
    user_satisfaction DECIMAL(3,2),
    sample_size INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_quality_tenant ON ai_quality_metrics(tenant_id, date DESC);
CREATE INDEX IF NOT EXISTS idx_ai_quality_feature ON ai_quality_metrics(feature);

-- DB.html 26.4 ai_performance_metrics
CREATE TABLE ai_performance_metrics (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    date DATE,
    model_used TEXT,
    p50_latency_ms INTEGER,
    p95_latency_ms INTEGER,
    p99_latency_ms INTEGER,
    throughput_rps DECIMAL(19,4),
    error_rate DECIMAL(3,2),
    success_rate DECIMAL(3,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_performance_tenant ON ai_performance_metrics(tenant_id, date DESC);
CREATE INDEX IF NOT EXISTS idx_ai_performance_model ON ai_performance_metrics(model_used);

-- DB.html 26.5 ai_alerts
CREATE TABLE ai_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    alert_type TEXT,
    severity TEXT,
    alert_message TEXT,
    alert_data JSONB DEFAULT '{}'::jsonb,
    resolved BOOLEAN DEFAULT false,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_alerts_tenant ON ai_alerts(tenant_id);
CREATE INDEX IF NOT EXISTS idx_ai_alerts_type ON ai_alerts(alert_type);
CREATE INDEX IF NOT EXISTS idx_ai_alerts_resolved ON ai_alerts(resolved) WHERE resolved = false;

-- DB.html 26.6 prompt_drift_logs
CREATE TABLE prompt_drift_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    prompt_template_id UUID,
    old_prompt TEXT,
    new_prompt TEXT,
    embedding_drift_score DECIMAL(3,2),
    performance_drift_score DECIMAL(3,2),
    change_reason TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_prompt_drift_tenant ON prompt_drift_logs(tenant_id);
CREATE INDEX IF NOT EXISTS idx_prompt_drift_template ON prompt_drift_logs(prompt_template_id);
