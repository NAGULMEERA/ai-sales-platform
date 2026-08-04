-- LeadRepository.search() filters on LOWER(customer_name/phone/email) LIKE LOWER('%q%'),
-- a leading-wildcard substring match. Neither a plain btree index nor the existing
-- idx_leads_fts (tsvector) GIN index (V003__lead_indexes.sql) can serve this predicate
-- shape. pg_trgm is already enabled (V001__foundation.sql); GIN trigram indexes on the
-- lower-cased expressions make these scans index-assisted instead of full table scans.
CREATE INDEX IF NOT EXISTS idx_leads_customer_name_trgm ON leads USING gin (lower(customer_name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_leads_phone_trgm ON leads USING gin (lower(phone) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_leads_email_trgm ON leads USING gin (lower(coalesce(email, '')) gin_trgm_ops);
