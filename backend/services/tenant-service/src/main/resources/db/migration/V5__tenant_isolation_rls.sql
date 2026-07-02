-- Story B: tenant isolation — soft-delete audit, DB constraints, UUID tenant_id, PostgreSQL RLS.

ALTER TABLE tenants ADD COLUMN deleted_at TIMESTAMP;
ALTER TABLE tenants ADD COLUMN deleted_by VARCHAR(255);

ALTER TABLE tenants ADD CONSTRAINT ck_tenants_status
    CHECK (status IN ('ACTIVE', 'SUSPENDED'));

ALTER TABLE tenants ADD CONSTRAINT ck_tenants_industry
    CHECK (industry IN ('REAL_ESTATE', 'AUTOMOBILE', 'EDUCATION', 'HEALTHCARE'));

ALTER TABLE tenants ADD CONSTRAINT ck_tenants_language
    CHECK (language <> '');

ALTER TABLE tenants ADD CONSTRAINT ck_tenants_timezone
    CHECK (timezone <> '');

-- Align tenant-scoped tables with UUID tenant_id for RLS policies.
ALTER TABLE users ADD COLUMN tenant_id_uuid UUID;
UPDATE users SET tenant_id_uuid = tenant_id::uuid;
ALTER TABLE users DROP COLUMN tenant_id;
ALTER TABLE users RENAME COLUMN tenant_id_uuid TO tenant_id;
ALTER TABLE users ALTER COLUMN tenant_id SET NOT NULL;

ALTER TABLE roles ADD COLUMN tenant_id_uuid UUID;
UPDATE roles SET tenant_id_uuid = tenant_id::uuid;
ALTER TABLE roles DROP COLUMN tenant_id;
ALTER TABLE roles RENAME COLUMN tenant_id_uuid TO tenant_id;
ALTER TABLE roles ALTER COLUMN tenant_id SET NOT NULL;

CREATE INDEX idx_tenant_users_tenant_id_uuid ON users(tenant_id);
CREATE INDEX idx_tenant_roles_tenant_id_uuid ON roles(tenant_id);

-- Row-Level Security for tenant-owned tables (platform admins bypass via app.is_platform_admin).
ALTER TABLE users ENABLE ROW LEVEL SECURITY;
ALTER TABLE users FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_users_isolation ON users
    USING (
        current_setting('app.is_platform_admin', true) = 'true'
        OR tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid
    )
    WITH CHECK (
        current_setting('app.is_platform_admin', true) = 'true'
        OR tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid
    );

ALTER TABLE roles ENABLE ROW LEVEL SECURITY;
ALTER TABLE roles FORCE ROW LEVEL SECURITY;

CREATE POLICY tenant_roles_isolation ON roles
    USING (
        current_setting('app.is_platform_admin', true) = 'true'
        OR tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid
    )
    WITH CHECK (
        current_setting('app.is_platform_admin', true) = 'true'
        OR tenant_id = NULLIF(current_setting('app.current_tenant', true), '')::uuid
    );
