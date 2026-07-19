-- Daily rollups are aggregated by tenant/day/metric only (dimensions belong on fact rows).
-- Removes JSONB from unique key and enables concurrent upserts.

ALTER TABLE analytics_daily_rollup
    DROP CONSTRAINT IF EXISTS uq_analytics_daily_rollup;

ALTER TABLE analytics_daily_rollup
    ADD CONSTRAINT uq_analytics_daily_rollup
    UNIQUE (tenant_id, metric_date, metric_name);
