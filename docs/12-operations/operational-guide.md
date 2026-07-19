# Operational Guide

Day-to-day production operations for the AI Sales Platform.

## Ownership map

| Concern | Where to look |
|---------|----------------|
| Service health | Actuator health groups; K8s probes |
| Auth / JWT | identity-service JWKS + gateway JWT validation |
| Event lag / DLT | [eventing-ops-matrix.md](eventing-ops-matrix.md) |
| Backups | [backup-and-restore.md](backup-and-restore.md) |
| Alerts / SLOs | [alerting-and-slos.md](alerting-and-slos.md) |
| Incidents | [incident-response-runbook.md](incident-response-runbook.md) |
| DR | [disaster-recovery-guide.md](disaster-recovery-guide.md) |

## Health checks

Each Spring Boot service exposes Actuator endpoints. Public exposure should be limited to health/info (not full `/actuator/**`) per security hardening.

Typical probes (K8s):

- Startup / liveness / readiness against health endpoints
- Dependency readiness (DB, Kafka, Redis) via health groups where configured

## Common operational tasks

### Rotate JWT signing keys

1. Provision new RSA key material via secrets manager / K8s secrets
2. Deploy identity-service with new keys; ensure JWKS serves current public key
3. Roll gateway and resource servers so validation accepts new keys
4. Invalidate sessions if rotation policy requires

### Replay / inspect failed events

1. Identify service from [eventing-ops-matrix.md](eventing-ops-matrix.md)
2. Inspect `dead_letter` / inbox tables in that service’s DB
3. Fix poison payload or consumer bug
4. Re-publish or reset inbox claim per service runbook practice (idempotent consumers required)

### Scale read-heavy APIs

- **search-service** and **analytics-service** are projection/query services — scale replicas independently of lead write path
- Watch Kafka consumer lag for indexer / facts consumers

### AI cost / quota

- Monitor token usage APIs and analytics `ai/usage`
- Adjust tenant quotas via `/api/v1/ai-quota`
- Provider outages: stub/fallback behavior is provider-router specific inside ai-service

## Observability stack (local compose)

Prometheus scrape + Alertmanager are defined in infra compose. Structured logs include `correlation_id`, `trace_id`, `tenant_id` when MDC is populated by the security filter.

## Change management

- Prefer rolling deploy with Flyway expand/contract ([rolling-deploy-and-flyway.md](../11-devops/rolling-deploy-and-flyway.md))
- Architecture freeze rules: [adr-030](../15-adr/adr-030-architecture-freeze-and-vertical-validation.md)

## Related runbooks

- [incident-response-runbook.md](incident-response-runbook.md) — severity, triage, escalation
- [disaster-recovery-guide.md](disaster-recovery-guide.md) — restore order and validation
