-- CustomerRepository.search() orders by created_at DESC (with Pageable LIMIT/OFFSET) for every
-- call, including the common case where :q is blank (browse-all). No existing index covers
-- (tenant_id, created_at); idx_customers_tenant_status only orders by tenant_id + status.
CREATE INDEX IF NOT EXISTS idx_customers_tenant_created
    ON customers (tenant_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- CustomerRepository.findIdentityCandidates / findPotentialDuplicates match
-- LOWER(c.email) = LOWER(:email), a case-insensitive equality lookup used on every customer
-- create/update to detect duplicates. This defeats the plain idx_customers_tenant_email index
-- (V002__customer_core.sql); a functional index on the lower-cased expression restores index use.
CREATE INDEX IF NOT EXISTS idx_customers_tenant_email_lower
    ON customers (tenant_id, lower(email))
    WHERE deleted_at IS NULL AND email IS NOT NULL;
