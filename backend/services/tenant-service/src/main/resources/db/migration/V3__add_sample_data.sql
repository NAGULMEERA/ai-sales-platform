-- Local/dev seed data only. tenants.plan/status match Tenant.TenantStatus (V1__create_tenants.sql).
INSERT INTO tenants (id, name, slug, plan, status, created_at, updated_at)
VALUES ('11111111-1111-1111-1111-111111111111', 'Demo Tenant', 'demo', 'FREE', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO roles (id, tenant_id, name, description, created_at, updated_at)
VALUES ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'ADMIN', 'Tenant administrator', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;

INSERT INTO permissions (id, code, description, created_at, updated_at)
VALUES ('33333333-3333-3333-3333-333333333333', 'TENANT_READ', 'Read tenant data', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT DO NOTHING;
