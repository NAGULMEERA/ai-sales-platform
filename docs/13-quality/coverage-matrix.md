# Test Coverage Matrix

Last updated: 2026-07-19

## Gap summary (before this pass)

| Area | Gap | Priority |
|------|-----|----------|
| Security | `JwtAuthenticationFilter`, `TenantAuthorizationAspect` untested | High |
| Tenant isolation | Cross-tenant aspect path untested | High |
| Workflow events | `LeadLifecycleEventConsumer` untested | High |
| Search events | `SearchIndexEventConsumer` untested | High |
| Analytics events | `AnalyticsEventConsumer` untested | High |
| Workflow actions | `WorkflowActionExecutor` untested | High |
| AI / prompt injection | `PromptVariableSanitizer` untested | High |
| RAG concurrency | Hybrid fusion only unit-tested serially | Medium |
| Repositories / Flyway | Search, analytics, workflow missing Testcontainers IT | Medium |
| Controllers | Almost no MockMvc across thin services | Medium (follow-up) |
| Appointment / Audit | Arch-only scaffolds | Low |

## Coverage by category (current target)

| Category | Status | Primary tests |
|----------|--------|---------------|
| Untested services (thin) | Improved | Search/Analytics consumers + Flyway IT |
| Untested aggregates | Partial | Existing lead/identity domain tests; workflow rules via engine tests |
| Untested workflows | Improved | `LeadLifecycleEventConsumerTest`, `WorkflowActionExecutorTest` |
| Untested events | Improved | Search/Analytics/Workflow consumer unit tests |
| Untested AI orchestration | Strong | Existing `AiGatewayServiceTest`, `AiQualificationOrchestratorTest` |
| Untested RAG | Improved | Existing Hybrid/Knowledge tests + concurrency + sanitizer |
| Untested repositories | Improved | Search/Analytics/Workflow Flyway migration ITs |
| Untested security | Improved | `JwtAuthenticationFilterTest`, JWT permission mapping |
| Untested tenant isolation | Improved | `TenantAuthorizationAspectTest`, tenant header mismatch in filter |

## New tests added in this pass

### Unit
- `common-security`: `JwtAuthenticationFilterTest`, `TenantAuthorizationAspectTest`
- `ai-service`: `PromptVariableSanitizerTest`, `PromptVariableSanitizerPerformanceTest`
- `ai-service`: `HybridRetrieverConcurrencyTest`
- `workflow-service`: `LeadLifecycleEventConsumerTest`, `WorkflowActionExecutorTest`
- `search-service`: `SearchIndexEventConsumerTest`
- `analytics-service`: `AnalyticsEventConsumerTest`

### Integration (Testcontainers)
- `SearchFlywayMigrationIT`
- `AnalyticsFlywayMigrationIT`
- `WorkflowFlywayMigrationIT`
- Require Docker (`disabledWithoutDocker = true`); skipped cleanly when Docker is unavailable

### Architecture
- Existing `ArchitectureTest` + `LayeredArchitectureRules` (JDK 26+ disabled where needed)

## Maintainability rules

1. Prefer Mockito unit tests for Kafka consumers (stub `IntegrationEventListener.handleIfType`).
2. Reuse `PlatformTestcontainers` — do not invent new container factories.
3. Assert table presence in Flyway ITs, not brittle migration counts.
4. Tag expensive suites with `@Tag("performance")` / `@Tag("concurrency")`.
5. Keep controller MockMvc for a dedicated follow-up once permission seeds stabilize.

## Follow-up backlog

- MockMvc security tests for AI/Search/Analytics controllers (`@PreAuthorize`)
- Repository slice tests for `SearchDocumentRepository` tenant filters
- Appointment/Audit business tests when aggregates land
- End-to-end Kafka IT (outbox → inbox) for search/analytics (reuse `OutboxToInboxIntegrationIT` pattern)
