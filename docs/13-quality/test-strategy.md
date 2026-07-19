# Test Strategy

## Unit Tests
- Service and mapper logic with JUnit 5 + Mockito
- Extend `BaseUnitTest` from common-testing
- Kafka consumers: stub `IntegrationEventListener.handleIfType` (see consumer tests in workflow/search/analytics)

## Integration Tests
- Extend `BaseIntegrationTest` with Testcontainers (PostgreSQL)
- Annotate with `@IntegrationTest`
- Prefer `PlatformTestcontainers.postgres(...)` and assert table presence (not brittle migration counts)
- `@Testcontainers(disabledWithoutDocker = true)` so local runs without Docker still pass unit suites

## Contract Tests
- OpenAPI specs in `docs/api/` as source of truth

## Performance / Concurrency
- Tag with `@Tag("performance")` or `@Tag("concurrency")`
- Keep budgets loose enough for CI variance

## Architecture
- `LayeredArchitectureRules` via each service `ArchitectureTest`
- Disable ArchUnit on JDK 26+ where required

## Coverage matrix
- See [coverage-matrix.md](./coverage-matrix.md) for gaps, new tests, and follow-up backlog

## CI
- All tests run on PR via GitHub Actions (`.github/workflows/test.yml`)
- Minimum 70% coverage target for business services
