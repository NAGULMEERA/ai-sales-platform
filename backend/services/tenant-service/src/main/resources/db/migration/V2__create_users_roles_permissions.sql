-- Tenant-scoped users, roles and permissions used by the tenant-service admin/RBAC read model.
-- tenant_id is stored as VARCHAR (not a FK to tenants.id) because these rows are looked up by the
-- string tenant identifier resolved from the JWT/TenantContext, matching User.tenantId/Role.tenantId.

CREATE TABLE users (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX idx_tenant_users_tenant_id ON users(tenant_id);

CREATE TABLE roles (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);

CREATE INDEX idx_tenant_roles_tenant_id ON roles(tenant_id);

CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    code VARCHAR(255) NOT NULL UNIQUE,
    description VARCHAR(512) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    updated_by VARCHAR(255)
);
