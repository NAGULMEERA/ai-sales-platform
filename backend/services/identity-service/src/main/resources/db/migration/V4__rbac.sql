-- Global permission catalog
CREATE TABLE permissions (
    id UUID PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_permissions_code ON permissions(code);
CREATE INDEX idx_permissions_category ON permissions(category);

-- Role-to-permission mapping (role names align with user_roles.role values)
CREATE TABLE role_permissions (
    id UUID PRIMARY KEY,
    role_name VARCHAR(50) NOT NULL,
    permission_id UUID NOT NULL REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    UNIQUE (role_name, permission_id)
);

CREATE INDEX idx_role_permissions_role ON role_permissions(role_name);

-- Seed core permissions
INSERT INTO permissions (id, code, name, description, category) VALUES
    (gen_random_uuid(), 'user:read', 'Read Users', 'View user profiles', 'user'),
    (gen_random_uuid(), 'user:create', 'Create Users', 'Create new users', 'user'),
    (gen_random_uuid(), 'user:update', 'Update Users', 'Update user profiles', 'user'),
    (gen_random_uuid(), 'user:delete', 'Delete Users', 'Deactivate users', 'user'),
    (gen_random_uuid(), 'lead:read', 'Read Leads', 'View leads', 'lead'),
    (gen_random_uuid(), 'lead:create', 'Create Leads', 'Create leads', 'lead'),
    (gen_random_uuid(), 'lead:update', 'Update Leads', 'Update leads', 'lead'),
    (gen_random_uuid(), 'lead:delete', 'Delete Leads', 'Delete leads', 'lead'),
    (gen_random_uuid(), 'tenant:admin', 'Tenant Admin', 'Full tenant administration', 'tenant'),
    (gen_random_uuid(), 'billing:read', 'Read Billing', 'View subscription and billing', 'billing'),
    (gen_random_uuid(), 'billing:manage', 'Manage Billing', 'Manage subscription plans', 'billing');

-- Seed role-permission assignments
INSERT INTO role_permissions (id, role_name, permission_id)
SELECT gen_random_uuid(), 'USER', p.id FROM permissions p
WHERE p.code IN ('user:read', 'lead:read', 'lead:create', 'lead:update');

INSERT INTO role_permissions (id, role_name, permission_id)
SELECT gen_random_uuid(), 'TENANT_ADMIN', p.id FROM permissions p;

INSERT INTO role_permissions (id, role_name, permission_id)
SELECT gen_random_uuid(), 'ADMIN', p.id FROM permissions p;
