-- Business analytics facts + daily rollups (tenant-scoped).

ALTER TABLE analytics_event
    ADD COLUMN IF NOT EXISTS metric_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS metric_value DOUBLE PRECISION NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS dimensions JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(255);

UPDATE analytics_event SET metric_name = 'UNKNOWN' WHERE metric_name IS NULL;
ALTER TABLE analytics_event ALTER COLUMN metric_name SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_analytics_event_tenant_metric_time
    ON analytics_event (tenant_id, metric_name, occurred_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_analytics_event_tenant_day
    ON analytics_event (tenant_id, (occurred_at::date), metric_name)
    WHERE deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS analytics_daily_rollup (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    organization_id UUID,
    metric_date DATE NOT NULL,
    metric_name VARCHAR(100) NOT NULL,
    metric_sum DOUBLE PRECISION NOT NULL DEFAULT 0,
    metric_count BIGINT NOT NULL DEFAULT 0,
    dimensions JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_analytics_daily_rollup UNIQUE (tenant_id, metric_date, metric_name, dimensions)
);

CREATE INDEX IF NOT EXISTS idx_analytics_daily_rollup_tenant_date
    ON analytics_daily_rollup (tenant_id, metric_date DESC);
