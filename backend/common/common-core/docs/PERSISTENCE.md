# Multi-tenant persistence (RLS + audit)

Platform patterns for PostgreSQL tenant isolation and local audit trails.

## Row-Level Security

1. Ensure entities extend `TenantAwareEntity` (Hibernate filter + `tenant_id` column).
2. Copy `common-core/src/main/resources/db/platform/V2__platform_tenant_rls_helpers.sql` into the service Flyway migrations (once per database).
3. After creating each tenant-owned table, enable RLS:

```sql
SELECT platform_enable_tenant_rls('public', 'leads');
```

`TenantRlsConnectionInitializer` sets session variables on each transaction:

- `app.current_tenant` — from `TenantContext`
- `app.is_platform_admin` — bypass for SUPER_ADMIN

Reference implementation: `tenant-service` migration `V5__tenant_isolation_rls.sql`.

## Local audit log

Copy `common-core/src/main/resources/db/platform/V1__platform_service_audit_log.sql` into service migrations, or use a service-specific table name (e.g. `tenant_audit_log`).

Wire `@Auditable` methods to a custom `AuditRecorder` bean, or rely on the platform default (see below).

## Hibernate tenant filter

`TenantHibernateFilterEnabler` activates the `tenantFilter` on `TenantAwareEntity` queries when a tenant id is present in context.
