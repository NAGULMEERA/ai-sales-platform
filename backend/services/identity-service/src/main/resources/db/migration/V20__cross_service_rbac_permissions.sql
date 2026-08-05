-- Permissions for sensitive mutating endpoints that previously relied only on
-- "authenticated()" (any logged-in user in the tenant) with no RBAC gate:
-- billing invoicing/payment, AI quota administration, marketplace plugin
-- enablement, customer delete/merge, and opportunity/quote close operations.
-- Not granted to USER; TENANT_ADMIN/ADMIN get them explicitly below, and
-- SUPER_ADMIN is already covered by the hasAnyRole(...) fallback used in the
-- corresponding @PreAuthorize expressions.
INSERT INTO permissions (id, code, name, description, category) VALUES
    (gen_random_uuid(), 'ai:quota:manage', 'Manage AI Quota', 'Upsert tenant AI token budgets', 'ai'),
    (gen_random_uuid(), 'plugin:manage', 'Manage Plugins', 'Enable, disable, and configure tenant plugins', 'marketplace'),
    (gen_random_uuid(), 'customer:delete', 'Delete Customers', 'Soft-delete and merge customers', 'customer'),
    (gen_random_uuid(), 'opportunity:close', 'Close Opportunities', 'Close, reopen opportunities and accept quotes', 'deal'),
    (gen_random_uuid(), 'opportunity:delete', 'Delete Opportunities', 'Archive (soft-delete) opportunities', 'deal')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (id, role_name, permission_id)
SELECT gen_random_uuid(), 'TENANT_ADMIN', p.id FROM permissions p
WHERE p.code IN ('ai:quota:manage', 'plugin:manage', 'customer:delete', 'opportunity:close', 'opportunity:delete')
ON CONFLICT (role_name, permission_id) DO NOTHING;

INSERT INTO role_permissions (id, role_name, permission_id)
SELECT gen_random_uuid(), 'ADMIN', p.id FROM permissions p
WHERE p.code IN ('ai:quota:manage', 'plugin:manage', 'customer:delete', 'opportunity:close', 'opportunity:delete')
ON CONFLICT (role_name, permission_id) DO NOTHING;
