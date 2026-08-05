-- CustomerRepository.search() filters on LOWER(full_name/email)/plain(phone/whatsapp)
-- LIKE '%q%', a leading-wildcard substring match. None of the existing equality indexes
-- (idx_customers_tenant_phone, idx_customers_tenant_email, ...) can serve this predicate
-- shape, and a plain btree index cannot support a leading wildcard. pg_trgm is a built-in,
-- non-breaking Postgres extension; GIN trigram indexes make these scans index-assisted
-- instead of full table scans.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_customers_full_name_trgm ON customers USING gin (lower(full_name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_customers_email_trgm ON customers USING gin (lower(coalesce(email, '')) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_customers_phone_trgm ON customers USING gin (coalesce(phone, '') gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_customers_whatsapp_trgm ON customers USING gin (coalesce(whatsapp, '') gin_trgm_ops);
