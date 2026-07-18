-- Configurable sales pipelines. Stage codes align with lead_status for the default pipeline.
-- Industry/plugin metadata may seed alternate pipelines later; Lead stays industry-agnostic.

CREATE TABLE sales_pipeline (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_sales_pipeline_tenant_code UNIQUE (tenant_id, code)
);

CREATE UNIQUE INDEX uq_sales_pipeline_tenant_default
    ON sales_pipeline (tenant_id)
    WHERE is_default = TRUE AND active = TRUE;

CREATE INDEX idx_sales_pipeline_tenant ON sales_pipeline (tenant_id);

CREATE TABLE sales_pipeline_stage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pipeline_id UUID NOT NULL REFERENCES sales_pipeline (id),
    stage_code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    stage_order INT NOT NULL,
    terminal BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uk_sales_pipeline_stage_code UNIQUE (pipeline_id, stage_code)
);

CREATE INDEX idx_sales_pipeline_stage_pipeline ON sales_pipeline_stage (pipeline_id);

CREATE TABLE sales_pipeline_transition (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pipeline_id UUID NOT NULL REFERENCES sales_pipeline (id),
    from_stage VARCHAR(50) NOT NULL,
    to_stage VARCHAR(50) NOT NULL,
    CONSTRAINT uk_sales_pipeline_transition UNIQUE (pipeline_id, from_stage, to_stage)
);

CREATE INDEX idx_sales_pipeline_transition_pipeline ON sales_pipeline_transition (pipeline_id);

ALTER TABLE leads
    ADD COLUMN pipeline_id UUID REFERENCES sales_pipeline (id);

CREATE INDEX idx_leads_pipeline_id ON leads (pipeline_id);
