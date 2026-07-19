-- List leads by tenant + created_at (soft-delete aware). Complements idx_leads_cover (tenant, status).
CREATE INDEX IF NOT EXISTS idx_leads_tenant_created
    ON leads (tenant_id, created_at DESC)
    WHERE deleted_at IS NULL;
