-- LeadRepository.findPotentialDuplicates() filters on l.sourceType = :sourceType AND
-- l.sourceId = :sourceId (LeadRepository.java) with no supporting index; leads only had
-- tenant/status/assigned/phone/email/external_id/score indexes (V002__lead_tables.sql).
CREATE INDEX IF NOT EXISTS idx_leads_tenant_source ON leads (tenant_id, source_type, source_id);

-- findPotentialDuplicates() also matches LOWER(l.email) = LOWER(:email), which defeats the
-- plain idx_leads_email index (a function on the indexed column prevents its use). A functional
-- index on the lower-cased expression lets Postgres use an index scan for this equality lookup.
CREATE INDEX IF NOT EXISTS idx_leads_email_lower ON leads (tenant_id, lower(email)) WHERE email IS NOT NULL;

-- LeadDuplicateRepository.findByTenantIdAndResolvedOrderByDetectedAtDesc (LeadService.listDuplicates)
-- filters tenant_id + resolved and orders by detected_at; only idx_lead_duplicates_tenant and the
-- partial idx_lead_duplicates_resolved (resolved = false) existed (V002__lead_tables.sql), neither
-- of which covers the ORDER BY, so every call required an extra sort pass.
CREATE INDEX IF NOT EXISTS idx_lead_duplicates_tenant_resolved_detected
    ON lead_duplicates (tenant_id, resolved, detected_at DESC);

-- LeadTagRepository.findByTenantIdAndTagIgnoreCase compares tag case-insensitively; the existing
-- idx_lead_tags_tag index is on the raw column and cannot serve a case-insensitive lookup.
CREATE INDEX IF NOT EXISTS idx_lead_tags_tenant_tag_lower ON lead_tags (tenant_id, lower(tag));
