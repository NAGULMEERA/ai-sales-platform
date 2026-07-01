# Multi-Tenant Isolation Strategy

## Decision

| Tier | Mode | When |
|------|------|------|
| **Default** | Shared DB + shared schema + `tenant_id` + **RLS** | All standard tenants |
| **Enterprise** | Shared DB + **dedicated schema** | Self-service upgrade, regulatory |
| **Premium** | **Dedicated database** | High security / large enterprise |

**Default for all new tenants:** `SHARED_SCHEMA` with PostgreSQL Row-Level Security and application-layer enforcement.

---

## Defense in Depth

```
JWT (tenant_id claim)
    ↓
TenantContext (ThreadLocal)
    ↓
@PreAuthorizeTenant aspect (cross-tenant block)
    ↓
Hibernate tenantFilter (TenantAwareEntity)
    ↓
PostgreSQL RLS (app.current_tenant session variable)
```

---

## Implementation

### 1. Application context

- `TenantContext` — `backend/common/common-core/.../TenantContext.java`
- Populated from JWT in `JwtAuthenticationFilter`
- Cleared after each request

### 2. Hibernate filter

```java
@FilterDef(name = "tenantFilter", parameters = @ParamDef(name = "tenantId", type = UUID.class))
@Filter(name = "tenantFilter", condition = "tenant_id = :tenantId")
public abstract class TenantAwareEntity extends BaseEntity { ... }
```

### 3. PostgreSQL RLS

```sql
CREATE POLICY tenant_isolation_policy ON leads
    USING (tenant_id = current_setting('app.current_tenant', true)::uuid);
```

Set per transaction via `TenantRlsConnectionInitializer`:

```sql
SELECT set_config('app.current_tenant', :tenantId, true);
```

Migration: `backend/database/migrations-monolith/V034__row_level_security.sql`

### 4. Tenant authorization aspect

`@PreAuthorizeTenant` on controllers/services blocks authenticated users from accessing another tenant's data.

---

## Enterprise Upgrades

| Mode | Provisioning |
|------|--------------|
| `DEDICATED_SCHEMA` | Create schema `tenant_{slug}`, migrate tenant data, update `tenant_isolation_config` |
| `DEDICATED_DATABASE` | Provision RDS instance, run Flyway, update connection routing |

Schema/database routing uses `tenant_isolation_config.isolation_mode`.

---

## Rules

- Every business table includes `tenant_id` (and `organization_id` where applicable).
- Never accept `tenant_id` from request body for authorization — use JWT only.
- Cross-tenant access must fail with `TENANT_002`.
- Every feature requires cross-tenant negative tests.

---

## Related

- ADR: [adr-007-multi-tenancy](../15-adr/adr-007-multi-tenancy.md)
- Code: `backend/common/common-core/.../persistence/`
- RLS migration: `V034__row_level_security.sql`
