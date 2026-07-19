-- Opportunity search/list ordered by updated_at (tenant-scoped soft-delete filter).
CREATE INDEX IF NOT EXISTS idx_opportunity_tenant_updated
    ON opportunity (tenant_id, updated_at DESC)
    WHERE deleted_at IS NULL;
