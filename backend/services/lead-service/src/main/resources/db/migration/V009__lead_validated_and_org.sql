-- Validation gate (assign requires validated=true) + optional organization scoping.

ALTER TABLE leads
    ADD COLUMN IF NOT EXISTS validated BOOLEAN NOT NULL DEFAULT false;

ALTER TABLE leads
    ADD COLUMN IF NOT EXISTS organization_id UUID;

CREATE INDEX IF NOT EXISTS idx_leads_tenant_validated ON leads(tenant_id, validated)
    WHERE deleted_at IS NULL;
