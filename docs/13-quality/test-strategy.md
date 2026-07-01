# Test Strategy

## Unit Tests
- Service and mapper logic with JUnit 5 + Mockito
- Extend `BaseUnitTest` from common-testing

## Integration Tests
- Extend `BaseIntegrationTest` with Testcontainers (PostgreSQL)
- Annotate with `@IntegrationTest`

## Contract Tests
- OpenAPI specs in `docs/api/` as source of truth

## CI
- All tests run on PR via GitHub Actions (`.github/workflows/test.yml`)
- Minimum 70% coverage target for business services
