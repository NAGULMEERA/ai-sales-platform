-- AI / search / conversation / media permissions for RBAC enforcement.
INSERT INTO permissions (id, code, name, description, category) VALUES
    (gen_random_uuid(), 'ai:execute', 'Execute AI', 'Invoke AI gateway execute/qualify', 'ai'),
    (gen_random_uuid(), 'ai:debug', 'AI Debug', 'Echo rendered prompts and debug AI payloads', 'ai'),
    (gen_random_uuid(), 'prompt:manage', 'Manage Prompts', 'Create and activate prompt templates', 'ai'),
    (gen_random_uuid(), 'knowledge:read', 'Read Knowledge', 'Read knowledge bases and documents', 'ai'),
    (gen_random_uuid(), 'knowledge:manage', 'Manage Knowledge', 'Ingest and manage knowledge documents', 'ai'),
    (gen_random_uuid(), 'search:read', 'Search', 'Use enterprise search APIs', 'search'),
    (gen_random_uuid(), 'analytics:read', 'Analytics', 'View analytics dashboards', 'analytics'),
    (gen_random_uuid(), 'conversation:read', 'Read Conversations', 'View conversations', 'conversation'),
    (gen_random_uuid(), 'conversation:write', 'Write Conversations', 'Create and update conversations', 'conversation'),
    (gen_random_uuid(), 'media:read', 'Read Media', 'Download media objects', 'media'),
    (gen_random_uuid(), 'media:write', 'Write Media', 'Upload media objects', 'media')
ON CONFLICT (code) DO NOTHING;

-- USER: operational permissions (no prompt/knowledge manage, no ai:debug)
INSERT INTO role_permissions (id, role_name, permission_id)
SELECT gen_random_uuid(), 'USER', p.id FROM permissions p
WHERE p.code IN (
    'ai:execute',
    'knowledge:read',
    'search:read',
    'analytics:read',
    'conversation:read',
    'conversation:write',
    'media:read',
    'media:write'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_name = 'USER' AND rp.permission_id = p.id
);

-- TENANT_ADMIN / ADMIN: all new permissions
INSERT INTO role_permissions (id, role_name, permission_id)
SELECT gen_random_uuid(), 'TENANT_ADMIN', p.id FROM permissions p
WHERE p.code IN (
    'ai:execute', 'ai:debug', 'prompt:manage', 'knowledge:read', 'knowledge:manage',
    'search:read', 'analytics:read', 'conversation:read', 'conversation:write',
    'media:read', 'media:write'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_name = 'TENANT_ADMIN' AND rp.permission_id = p.id
);

INSERT INTO role_permissions (id, role_name, permission_id)
SELECT gen_random_uuid(), 'ADMIN', p.id FROM permissions p
WHERE p.code IN (
    'ai:execute', 'ai:debug', 'prompt:manage', 'knowledge:read', 'knowledge:manage',
    'search:read', 'analytics:read', 'conversation:read', 'conversation:write',
    'media:read', 'media:write'
)
AND NOT EXISTS (
    SELECT 1 FROM role_permissions rp WHERE rp.role_name = 'ADMIN' AND rp.permission_id = p.id
);
