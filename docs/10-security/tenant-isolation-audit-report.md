# Tenant Isolation Audit Report

**Date:** 2026-08-05  
**Scope:** Full repository review of repositories, services, controllers, async/Kafka consumers, Feign propagation, caches, and gateway rate limiting  
**Mode:** Verified issues fixed; no business-logic redesign

---

## Executive summary

Tenant isolation is primarily enforced via **explicit `findByTenantId…` queries** and `TenantContext`, not Hibernate session filters. Several Critical/High IDOR and cross-tenant write paths were confirmed and fixed. Remaining systemic debt (unused Hibernate `tenantFilter`, lead child tables without `tenant_id`) is documented as follow-up hardening.

| Severity | Found | Fixed this pass |
|----------|-------|-----------------|
| Critical | 1 | 1 |
| High | 6 | 6 |
| Moderate | 6 | 6 |
| Systemic / deferred | 2 | 0 (tracked below) |

---

## Fixes delivered

### Critical

| ID | Issue | Fix |
|----|-------|-----|
| ID-1 | `identity-service` `GET /users/{id}` loaded any user by PK | `findByIdAndTenantId`; platform admin may cross-tenant; tests added |
| — | `tenant-service` user CRUD similarly unscoped | Same pattern via `TenantAuthorization` + `findByIdAndTenantId` |

### High

| ID | Issue | Fix |
|----|-------|-----|
| H1 | `ai-service` native embedding `UPDATE` missing `tenant_id` | Predicate + callers updated |
| H2 | `search-service` `refreshSearchVector` missing `tenant_id` | Predicate + callers updated |
| H3 | `workflow-service` engine `findById` ignored available `tenantId` | All reloads use `findByTenantIdAndId` |
| H4 | `billing-service` webhook invoice load via unscoped `findById` | `findByTenantIdAndIdAndDeletedAtIsNull(payment.tenantId, …)` |
| H5 | Feign configs copied headers only from servlet request | `TenantContext` fallback for tenant/org on 6 services (integration already correct) |
| H6 | `SemanticCacheService` similar-entry load via unscoped `findById` | `findByIdAndTenantId` |
| — | Lead Kafka conversation consumer wrote timeline without ownership check | Event `tenantId` + `requireOwnedLead` before side effects |

### Moderate

| ID | Issue | Fix |
|----|-------|-----|
| M1 | `PipelineController` / `SubscriptionController` missing `@PreAuthorizeTenant` | Class-level annotation added |
| M2 | Gateway tenant rate limiter fell back to IP and `denyEmptyKey=false` | `TenantPlanKeyResolver` returns empty without tenant; SaaS routes `denyEmptyKey=true` |
| M3 | Semantic cache invalidate deleted DB only | `evictNamespaceForTenant` on Redis via explicit-tenant pattern API |
| M4 | Lead post-create `findById`; pipeline transition cache keyed by `pipelineId` only | Tenant-scoped reload; cache key `(tenantId, pipelineId)` + ownership check |
| ID-3 | User create trusted `X-Tenant-Id` header | Service rejects non-platform-admin when header ≠ `TenantContext` |

---

## Deferred / follow-up (not fixed this pass)

| ID | Item | Rationale |
|----|------|-----------|
| SYS-1 | Most tenant entities do not extend `TenantAwareEntity` / lack `@Filter(tenantFilter)` | Broad entity/migration change; isolation today relies on explicit queries. Adopt filter as defense-in-depth in a dedicated PR. |
| LD-2 | Lead child tables (`LeadNote`, etc.) have no `tenant_id`; repos scoped by `leadId` only | HTTP paths gate via `requireLead` (tenant-scoped). Schema change + backfill required for filter/RLS on children. |
| ID-2 | `UserRepository.findByIdForUpdate` unscoped | Acceptable for login-by-email lockout path; document / tighten if reused from APIs. |

---

## Architecture notes

1. **`@PreAuthorizeTenant`** asserts JWT principal tenant == `TenantContext` — **not** resource ownership. IDOR protection must remain in service/repository `findByTenantIdAnd…` calls.
2. **`tenant-service` vs `identity-service` user ownership** overlaps architecturally; isolation was strengthened in both without merging ownership (out of scope).
3. **Hibernate `tenantFilter` enablement** currently no-ops for entities that do not declare the filter — do not treat filter enablement as sufficient isolation until SYS-1 is closed.

---

## Verification

Targeted module tests executed and passing for:

- `identity-service` / `tenant-service` user isolation
- `lead-service` conversation timeline + pipeline transition cache
- `workflow-service` automation engine
- `billing-service` `PaymentServiceTest`
- `ai-service` semantic cache / knowledge indexing
- `search-service` indexing
- `common-cache` key/eviction
- `api-gateway` `TenantPlanKeyResolverTest`

---

## Recommended next steps

1. Adopt `TenantAwareEntity` (or per-entity `@Filter`) across tenant aggregates (SYS-1).
2. Add `tenant_id` to lead child tables and tenant-scoped child repositories (LD-2).
3. Consider consolidating Feign interceptor into a shared common bean to avoid drift across the 7 copies.
4. Re-run a focused IDOR pass after SYS-1 lands (inherited `findById` becomes safer under the session filter).
