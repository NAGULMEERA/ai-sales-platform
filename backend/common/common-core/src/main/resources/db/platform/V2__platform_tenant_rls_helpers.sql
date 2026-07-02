-- Platform Infrastructure Epic — Phase 3
-- Install once per service database, then call from tenant-table migrations:
--   SELECT platform_enable_tenant_rls('public', 'leads');
--   SELECT platform_enable_tenant_rls('public', 'leads', 'lead_tenant_isolation');

CREATE OR REPLACE FUNCTION platform_enable_tenant_rls(
    p_schema text,
    p_table text,
    p_policy_name text DEFAULT NULL
)
RETURNS void
LANGUAGE plpgsql
AS $$
DECLARE
    v_policy text;
BEGIN
    v_policy := COALESCE(p_policy_name, p_table || '_tenant_isolation');

    EXECUTE format('ALTER TABLE %I.%I ENABLE ROW LEVEL SECURITY', p_schema, p_table);
    EXECUTE format('ALTER TABLE %I.%I FORCE ROW LEVEL SECURITY', p_schema, p_table);
    EXECUTE format('DROP POLICY IF EXISTS %I ON %I.%I', v_policy, p_schema, p_table);
    EXECUTE format(
        'CREATE POLICY %I ON %I.%I USING (
            current_setting(''app.is_platform_admin'', true) = ''true''
            OR tenant_id = NULLIF(current_setting(''app.current_tenant'', true), '''')::uuid
        ) WITH CHECK (
            current_setting(''app.is_platform_admin'', true) = ''true''
            OR tenant_id = NULLIF(current_setting(''app.current_tenant'', true), '''')::uuid
        )',
        v_policy, p_schema, p_table
    );
END;
$$;
