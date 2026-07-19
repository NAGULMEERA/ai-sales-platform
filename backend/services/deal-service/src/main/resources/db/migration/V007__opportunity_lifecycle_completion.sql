-- Opportunity lifecycle completion: stages, scoring, catalog refs, notes, timeline.

ALTER TABLE opportunity DROP CONSTRAINT IF EXISTS ck_opportunity_status;
ALTER TABLE opportunity ADD CONSTRAINT ck_opportunity_status CHECK (
    status IN ('OPEN', 'QUALIFIED', 'QUOTED', 'NEGOTIATION', 'WON', 'LOST', 'CANCELLED')
);

ALTER TABLE opportunity ADD COLUMN IF NOT EXISTS score INT;
ALTER TABLE opportunity ADD COLUMN IF NOT EXISTS catalog_product_id UUID;
ALTER TABLE opportunity ADD COLUMN IF NOT EXISTS catalog_offer_id UUID;
ALTER TABLE opportunity ADD COLUMN IF NOT EXISTS notes TEXT;
ALTER TABLE opportunity ADD COLUMN IF NOT EXISTS close_reason VARCHAR(2000);

ALTER TABLE opportunity DROP CONSTRAINT IF EXISTS ck_opportunity_score;
ALTER TABLE opportunity ADD CONSTRAINT ck_opportunity_score CHECK (
    score IS NULL OR (score >= 0 AND score <= 100)
);

CREATE INDEX IF NOT EXISTS idx_opportunity_tenant_catalog_product
    ON opportunity (tenant_id, catalog_product_id)
    WHERE catalog_product_id IS NOT NULL AND deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS opportunity_timeline (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    opportunity_id UUID NOT NULL REFERENCES opportunity (id),
    event_type VARCHAR(80) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    details JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID
);

CREATE INDEX IF NOT EXISTS idx_opportunity_timeline_opp
    ON opportunity_timeline (tenant_id, opportunity_id, created_at DESC);
