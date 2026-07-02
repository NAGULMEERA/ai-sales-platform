-- Tenant isolation on primary lead aggregate and tenant-scoped reference tables.

SELECT platform_enable_tenant_rls('public', 'leads');
SELECT platform_enable_tenant_rls('public', 'lead_sources');
SELECT platform_enable_tenant_rls('public', 'lead_tags');
SELECT platform_enable_tenant_rls('public', 'lead_custom_fields');
SELECT platform_enable_tenant_rls('public', 'lead_duplicates');
