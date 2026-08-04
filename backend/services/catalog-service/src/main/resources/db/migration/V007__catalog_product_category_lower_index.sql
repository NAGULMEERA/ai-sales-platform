-- CatalogProductRepository.search() filters with LOWER(p.category) = LOWER(:category), a
-- case-insensitive equality match. This defeats the plain idx_catalog_product_tenant_category
-- (tenant_id, category) index (V002__catalog_core.sql) for the category predicate specifically.
-- A functional index on the lower-cased expression lets Postgres use an index scan again.
CREATE INDEX IF NOT EXISTS idx_catalog_product_tenant_category_lower
    ON catalog_product (tenant_id, lower(category));
