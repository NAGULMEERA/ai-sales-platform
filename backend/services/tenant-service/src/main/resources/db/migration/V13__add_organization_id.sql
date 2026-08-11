-- Align users/roles with TenantAwareEntity.organizationId (nullable org scoping).

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS organization_id UUID;

ALTER TABLE roles
    ADD COLUMN IF NOT EXISTS organization_id UUID;

CREATE INDEX IF NOT EXISTS idx_tenant_users_organization_id ON users(organization_id);
CREATE INDEX IF NOT EXISTS idx_tenant_roles_organization_id ON roles(organization_id);
