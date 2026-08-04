-- CatalogProductRepository.search() filters on LOWER(name)/LOWER(code) LIKE LOWER('%q%'),
-- a leading-wildcard substring match. None of the existing equality indexes
-- (idx_catalog_product_tenant_status/category/type) can serve this predicate shape, and a
-- plain btree index cannot support a leading wildcard. pg_trgm is a built-in, non-breaking
-- Postgres extension; GIN trigram indexes make these scans index-assisted instead of full
-- table scans.
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_catalog_product_name_trgm ON catalog_product USING gin (lower(name) gin_trgm_ops);
CREATE INDEX IF NOT EXISTS idx_catalog_product_code_trgm ON catalog_product USING gin (lower(code) gin_trgm_ops);
