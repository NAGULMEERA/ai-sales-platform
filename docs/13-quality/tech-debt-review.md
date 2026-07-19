# Technical Debt Review

Date: 2026-07-19  
Constraint: behavior-preserving cleanup only.

## Removed (safe, zero callers)

| Item | Evidence |
|------|----------|
| `contracts.feign.TenantServiceClient` + `contracts.dto.tenant.*` | Superseded by `contracts.client.TenantServiceClient` + `contracts.tenant.*`; no imports outside own package |
| `IdentityFeatureFlagEvaluator` | Only implementor of unused `FeatureFlagEvaluator`; nothing injected it |
| `FeatureFlagEvaluator` | No remaining implementors or consumers |
| `RolePermissionRepository` | Entity still used via `PermissionRepository` JPQL; repository never injected |
| `RoleRepository` (tenant-service) | Never injected; `Role` entity retained for schema/JPA mapping |
| `LeadInsightsDto`, `RecommendationResultDto` | Unused AI contract shells; catalog uses `CatalogRecommendationResultDto` |
| `RouteLocatorConfig` (api-gateway) | Empty `@Configuration`; routes live in `GatewayConfig` |
| `TestDataFactory` | Zero call sites in tests |

## Refactored (no behavior change)

| Change | Why |
|--------|-----|
| `EVENT_TYPE` on ~68 event models | Removes duplicated magic strings in `init` + Kafka consumers |
| Consumers use `XxxEvent.EVENT_TYPE` | Compile-time link between publisher and consumer type names |
| `Customer.syncPrimaryContact(ContactMethodType, …)` | Replaces string switch with existing enum |
| `AiGatewayService.resolveCacheModel` | Uses `OpenAiLlmClient.NAME` / `GeminiLlmClient.NAME` / `StubLlmProvider.NAME` |
| `ObjectStrings.nullSafe` | Shared helper; analytics/search consumers delegate to it |

## Deferred (risky / large)

| Item | Reason |
|------|--------|
| Split `CustomerService` (~813), `LeadService` (~660), `ConversationService` (~482) | High risk to TX boundaries; do as dedicated PR per capability |
| Delete appointment/audit scaffold services | May be wired in deploy/compose; confirm ops ownership first |
| Delete unused `tenant.domain.entity.Role` | Table/entity may be intentional for future RBAC in tenant BC |
| Controller MockMvc / unused public endpoints | Needs product confirmation of API surface |
| Merge near-duplicate DTOs (`AiLeadQualificationResultDto` vs `QualificationResultDto`) | Public contract compatibility risk |

## Code smell hotspots (leave for follow-up PRs)

1. God services listed above — extract by use-case, keep public method signatures
2. SpEL permission blobs repeated on controllers — introduce meta-annotations in `common-security`
3. Status string compares (`"LOST"`, `"CLOSED_WON"`) in analytics — map via deal/lead status enums when shared contracts allow
4. Fat Kafka consumers — registry/table of handlers keyed by `EVENT_TYPE`

## Unused migrations

No empty or orphaned Flyway files found that are safe to delete. Applied migrations must never be edited; superseding requires a new versioned migration.
