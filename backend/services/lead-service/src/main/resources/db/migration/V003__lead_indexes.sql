-- Lead Service: indexes (from DDS V004 + V028 subset)
CREATE INDEX IF NOT EXISTS idx_leads_metadata_gin ON leads USING gin (metadata);
CREATE INDEX IF NOT EXISTS idx_leads_cover ON leads(tenant_id, status) INCLUDE (score, assigned_to, created_at);
CREATE INDEX IF NOT EXISTS idx_leads_fts ON leads USING gin(
    to_tsvector('english', customer_name || ' ' || COALESCE(transcript, ''))
);
CREATE INDEX IF NOT EXISTS idx_leads_date_qualified ON leads(tenant_id, (metadata->>'qualified_at')::timestamptz)
    WHERE status = 'QUALIFIED';
