# Test Coverage Matrix

Last updated: 2026-07-19 (QA Architect pass)

## Coverage goal

| Target | Enforcement |
|--------|-------------|
| **85% line coverage** (platform goal) | Optional Maven profile: `mvn -Pcoverage-gate verify` |
| Default CI (`mvn test`) | JaCoCo **report** always; **check not enforced** (scaffold services would fail CI) |

Trade-off: appointment-service and audit-service are ArchUnit-only scaffolds. A hard 85% gate on every module would block merges until those domains ship. Progressive path: instrument + report everywhere → raise `-Pcoverage-gate` module-by-module.

## Category status

| Category | Status | Evidence |
|----------|--------|----------|
| Unit | Strong / expanding | Catalog lookup, Quote batch/idempotency, CatalogQuoteGateway, QuoteIdempotency |
| Integration / Testcontainers | Expanding | Catalog + Deal Flyway ITs (reuse `PlatformTestcontainers`) |
| Architecture | Present | Per-service `ArchitectureTest` + `LayeredArchitectureRules` |
| Security / JWT | Improved | Gateway `JwtAuthenticationFilterTest` + existing common-security filter/JWT suites |
| Tenant isolation | Improved | Lead + Catalog + Knowledge retrieval isolation |
| Concurrency | Present | `HybridRetrieverConcurrencyTest`, gateway load smoke |
| Performance / Load | Smoke | `@Tag("performance")` / `@Tag("load")` on gateway batch + sanitizer |
| Contract | Lightweight | `CatalogOfferLookupRequestContractTest` (Jakarta Validation) |
| API / MockMvc | Gap | Thin controllers still mostly untested (follow-up) |
| AI / RAG | Strong | Existing suite + tenant isolation |
| Workflow | Present | Engine + lifecycle consumers (prior pass) |
| Events / Kafka | Present | Outbox→Inbox IT + listener concurrency unit test |
| Appointment / Audit | Weak | ArchUnit only |

## Tests added this QA pass

### Unit
- `CatalogServiceTest` — `getOffersByIds` (dedupe, empty, max 100, tenant scope)
- `CatalogQuoteGatewayTest` — batch Feign lookup / failure wrapping
- `QuoteServiceTest` — multi-line batch lookup + idempotent cache hit
- `QuoteIdempotencyServiceTest` — cache hit / blank / expired
- `KnowledgeRetrievalTenantIsolationTest` — cross-tenant KB not found
- `KafkaEventsAutoConfigurationTest` — listener concurrency wiring
- Gateway `JwtAuthenticationFilterTest` — public bypass, 401, header propagation

### Contract
- `CatalogOfferLookupRequestContractTest`

### Integration (Testcontainers)
- `CatalogFlywayMigrationIT`
- `DealFlywayMigrationIT`

### Performance / load (tagged)
- `CatalogQuoteGatewayLoadTest` (`@Tag("performance")`, `@Tag("load")`)

### Tooling
- Parent `backend/pom.xml`: JaCoCo prepare-agent + report; profile `coverage-gate` (85% LINE)

## Follow-up backlog (toward sustained 85%)

1. MockMvc `@WebMvcTest` security for AI/Search/Lead controllers
2. Business tests for appointment/audit when aggregates land
3. Enable `-Pcoverage-gate` on common-security, common-events, deal, catalog, ai first
4. k6 / Gatling soak against gateway (out of unit CI)
5. OpenAPI contract runner once `docs/api/` specs are published
6. Repository slice IT for search document tenant filters

## Maintainability rules

1. Prefer Mockito unit tests for Kafka consumers (stub `IntegrationEventListener.handleIfType`).
2. Reuse `PlatformTestcontainers` — do not invent new container factories.
3. Assert table presence in Flyway ITs, not brittle migration counts.
4. Tag expensive suites with `@Tag("performance")` / `@Tag("concurrency")` / `@Tag("load")`.
5. Do not change business behaviour to inflate coverage.
