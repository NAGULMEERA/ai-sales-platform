-- Scope idempotency keys per tenant. Previously idempotency_key was globally unique,
-- so two tenants using the same client-generated key value could collide and one
-- tenant would be served the other tenant's cached response (cross-tenant data leak).
-- Existing unexpired rows cannot be safely attributed to a tenant retroactively;
-- they age out naturally via the existing expires_at TTL and are excluded from the
-- new lookup path (tenant_id IS NULL never matches a tenant-scoped query).

ALTER TABLE idempotency_keys ADD COLUMN IF NOT EXISTS tenant_id UUID;

ALTER TABLE idempotency_keys DROP CONSTRAINT IF EXISTS idempotency_keys_idempotency_key_key;

CREATE UNIQUE INDEX IF NOT EXISTS uq_idempotency_keys_tenant_key
    ON idempotency_keys(tenant_id, idempotency_key);

CREATE INDEX IF NOT EXISTS idx_idempotency_keys_tenant ON idempotency_keys(tenant_id);
