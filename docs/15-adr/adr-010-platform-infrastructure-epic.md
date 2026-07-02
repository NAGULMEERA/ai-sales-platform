# ADR-010: Platform Infrastructure Epic

## Status

Accepted

## Context

Business services (Lead, Customer, Conversation, etc.) require the same cross-cutting capabilities:

- Reliable event publishing (outbox) and consumption (inbox + DLQ)
- Observability (OpenTelemetry, standard metrics)
- Multi-tenant persistence (RLS, tenant-aware caching, audit)
- Consistent developer experience (scaffold, integration tests)

Implementing these per service would duplicate effort and risk architectural drift.

## Decision

Deliver platform capabilities in four phases via shared modules and adoption checklists:

| Phase | Module / artifact | Purpose |
|-------|-------------------|---------|
| 1 | `common-events` | Outbox, inbox, DLQ, Kafka auto-config |
| 2 | `common-observability` | OTel, `PlatformMetrics`, dashboards |
| 3 | `common-cache`, `common-core` persistence templates | Redis cache, RLS Flyway, default audit |
| 4 | `common-testing`, `scaffold-platform-service.ps1` | Integration test harness, service scaffold |

Reference implementations: `tenant-service`, `identity-service`, `lead-service` (platform skeleton).

End-to-end event pipeline validated by `OutboxToInboxIntegrationIT` in `common-events`.

## Consequences

### Positive

- New business services adopt platform capabilities via config + Flyway templates
- Lead Service can begin feature work on a consistent foundation
- Integration test harness documents expected event flow

### Negative

- Services must copy Flyway platform migrations (database-per-service)
- Docker required for full integration test suite

## References

- `docs/03-architecture/platform-infrastructure-epic.md`
- Rule 01 — Microservice Policy
- Rule 03 — Event Governance
