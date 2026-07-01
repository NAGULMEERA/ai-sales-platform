-- ============================================================================
-- V027__materialized_views.sql
-- DB.html v7 section 27
-- ============================================================================

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_lead_funnel_daily AS
SELECT tenant_id, status, COUNT(*) AS lead_count, DATE(created_at) AS metric_date
FROM leads
WHERE status NOT IN ('WON', 'LOST')
GROUP BY tenant_id, status, DATE(created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_lead_funnel_daily
    ON mv_lead_funnel_daily(tenant_id, status, metric_date);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_tenant_daily_metrics AS
SELECT tenant_id, DATE(created_at) AS metric_date,
    COUNT(*) FILTER (WHERE status = 'NEW') AS new_leads,
    COUNT(*) FILTER (WHERE status = 'QUALIFIED') AS qualified_leads,
    COUNT(*) FILTER (WHERE status = 'WON') AS won_leads
FROM leads
GROUP BY tenant_id, DATE(created_at);

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_tenant_daily_metrics
    ON mv_tenant_daily_metrics(tenant_id, metric_date);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_ai_cost_daily AS
SELECT tenant_id, date AS metric_date, SUM(total_cost) AS total_cost, SUM(total_tokens) AS total_tokens
FROM ai_cost_metrics
GROUP BY tenant_id, date;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_ai_cost_daily
    ON mv_ai_cost_daily(tenant_id, metric_date);

CREATE MATERIALIZED VIEW IF NOT EXISTS mv_ai_quality_daily AS
SELECT tenant_id, date AS metric_date, AVG(accuracy) AS avg_accuracy, AVG(hallucination_rate) AS avg_hallucination_rate
FROM ai_quality_metrics
GROUP BY tenant_id, date;

CREATE UNIQUE INDEX IF NOT EXISTS idx_mv_ai_quality_daily
    ON mv_ai_quality_daily(tenant_id, metric_date);
