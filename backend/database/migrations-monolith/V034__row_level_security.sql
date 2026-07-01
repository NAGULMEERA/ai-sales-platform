-- ============================================================================
-- V034__row_level_security.sql
-- PostgreSQL Row-Level Security for shared-schema multi-tenancy (default tier)
-- See docs/10-security/multi-tenant-isolation-strategy.md
-- ============================================================================

-- Tenant isolation tier registry (platform + enterprise upgrades)
CREATE TABLE IF NOT EXISTS tenant_isolation_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE,
    isolation_mode VARCHAR(50) NOT NULL DEFAULT 'SHARED_SCHEMA',
    schema_name VARCHAR(100),
    database_name VARCHAR(100),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_tenant_isolation_mode CHECK (
        isolation_mode IN ('SHARED_SCHEMA', 'DEDICATED_SCHEMA', 'DEDICATED_DATABASE')
    )
);

ALTER TABLE tenant_isolation_config
    ADD CONSTRAINT fk_tenant_isolation_config_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

-- Helper: apply RLS policy template to tenant-owned tables
CREATE OR REPLACE FUNCTION apply_tenant_rls_policy(p_table regclass) RETURNS void AS $$
DECLARE
    v_policy_name text := 'tenant_isolation_policy';
BEGIN
    EXECUTE format('ALTER TABLE %s ENABLE ROW LEVEL SECURITY', p_table);
    EXECUTE format('ALTER TABLE %s FORCE ROW LEVEL SECURITY', p_table);
    IF NOT EXISTS (
        SELECT 1 FROM pg_policies
        WHERE schemaname = split_part(p_table::text, '.', 1)
          AND tablename = split_part(p_table::text, '.', 2)
          AND policyname = v_policy_name
    ) THEN
        EXECUTE format(
            'CREATE POLICY %I ON %s USING (tenant_id = current_setting(''app.current_tenant'', true)::uuid)',
            v_policy_name, p_table
        );
    END IF;
END;
$$ LANGUAGE plpgsql;

-- Core business tables (shared schema default)
SELECT apply_tenant_rls_policy('leads'::regclass);
SELECT apply_tenant_rls_policy('customers'::regclass);
SELECT apply_tenant_rls_policy('knowledge_bases'::regclass);
SELECT apply_tenant_rls_policy('rag_pipelines'::regclass);
SELECT apply_tenant_rls_policy('semantic_cache'::regclass);
SELECT apply_tenant_rls_policy('prompt_templates'::regclass);
SELECT apply_tenant_rls_policy('agents'::regclass);
SELECT apply_tenant_rls_policy('subscriptions'::regclass);

-- Application sets: SELECT set_config('app.current_tenant', '{uuid}', true)
-- via TenantRlsConnectionInitializer in common-core
