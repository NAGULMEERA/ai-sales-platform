# Test Strategy

## Unit Tests
- Service and mapper logic with JUnit 5 + Mockito + AssertJ
- Prefer extending existing `*Test` classes over new parallel suites
- Kafka consumers: stub `IntegrationEventListener.handleIfType`

## Integration Tests
- Prefer `PlatformTestcontainers.postgres(...)` / `kafka()`
- Annotate with `@Testcontainers(disabledWithoutDocker = true)`
- Assert table presence after Flyway migrate (not brittle version counts)
- Identity critical paths remain the SpringBootTest reference (`IdentityIntegrationTestBase`)

## Architecture Tests
- `LayeredArchitectureRules` via each service `ArchitectureTest`
- Disable ArchUnit on JDK 26+ where required

## Security / JWT / Tenant
- common-security servlet filter + aspect tests
- api-gateway reactive JWT filter tests (token issuance via common-security test dependency)
- Service-level tenant isolation (Lead, Catalog, Knowledge retrieval)

## Contract Tests
- Lightweight Jakarta Validation contracts for additive DTOs until OpenAPI suite lands in `docs/api/`

## Performance / Concurrency / Load
- Tag with `@Tag("performance")`, `@Tag("concurrency")`, or `@Tag("load")`
- Keep budgets loose enough for CI variance; full soak is out-of-band (k6)

## AI / Workflow / Events
- AI: gateway, prompts, RAG, sanitizer, tenant isolation
- Workflow: automation engine + lifecycle consumers
- Events: unit publishers/listeners + `OutboxToInboxIntegrationIT`

## Coverage
- Goal: **85% line coverage** platform-wide
- Default: JaCoCo reports on `mvn test` (`target/site/jacoco/`)
- Optional gate: `mvn -Pcoverage-gate verify` (fails modules under 85%)
- See [coverage-matrix.md](./coverage-matrix.md)

## CI
- PR tests: `.github/workflows/test.yml` → `./mvnw test -B`
- Full verify: `.github/workflows/ci.yml`
- Script: `./scripts/run-tests.sh`
