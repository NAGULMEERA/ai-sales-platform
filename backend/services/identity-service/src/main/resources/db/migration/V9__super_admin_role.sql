-- Platform super-admin role and permissions (Story C).

INSERT INTO permissions (id, code, name, description, category) VALUES
    (gen_random_uuid(), 'tenant:create', 'Create Tenants', 'Create new platform tenants', 'platform'),
    (gen_random_uuid(), 'tenant:manage', 'Manage Tenants', 'Activate, suspend, and delete tenants', 'platform'),
    (gen_random_uuid(), 'platform:admin', 'Platform Admin', 'Full platform administration', 'platform')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (id, role_name, permission_id)
SELECT gen_random_uuid(), 'SUPER_ADMIN', p.id
FROM permissions p
WHERE p.code IN ('tenant:create', 'tenant:manage', 'platform:admin')
ON CONFLICT (role_name, permission_id) DO NOTHING;
