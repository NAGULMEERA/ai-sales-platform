# Incident Response Runbook

## Severity

| Severity | Definition | Response |
|----------|------------|----------|
| Critical | Platform outage, auth broken, data loss risk | Page on-call immediately |
| High | Major feature down (AI, lead create, payments) | Respond within 15 minutes |
| Medium | Degraded performance or partial failure | Respond within 1 hour |
| Low | Cosmetic / single-tenant annoyance | Next business day |

## First 15 minutes

1. **Declare** severity and incident channel.
2. **Check** Grafana dashboards + Prometheus alerts (`ServiceDown`, `HighHttp5xxRate`, `CircuitBreakerOpen`, `OutboxLagHigh`).
3. **Identify** blast radius: gateway vs single service vs data plane (Postgres/Kafka/Redis).
4. **Stabilize** — scale healthy replicas, disable failing feature flag, or fail closed (do not bypass tenant/security controls).
5. **Communicate** status to stakeholders.

## Common failure modes

| Symptom | Likely cause | Actions |
|---------|--------------|---------|
| All APIs 401 | JWT keys / JWKS / identity down | Check identity pods, JWKS, secrets |
| 503 on one service | Crash loop / readiness fail | `kubectl describe`, logs, DB connectivity |
| High latency | Pool exhaustion / Kafka lag / AI provider | Hikari metrics, outbox lag, AI CB state |
| Duplicate side effects | Inbox/outbox misconfig | Verify inbox enabled; inspect DLT |
| AI stub in prod | Missing provider keys | Startup validation / ProdLlmGuard logs |

## Escalation

1. Service owner (bounded context)
2. Platform on-call (Kafka/Postgres/K8s)
3. Security on-call (auth, tenant isolation, secret exposure)

## Post-incident

- Timeline, root cause, customer impact
- Corrective actions with owners and due dates
- Update alerts/runbooks if detection was slow

## Related

- [alerting-and-slos.md](alerting-and-slos.md)
- [eventing-ops-matrix.md](eventing-ops-matrix.md)
- [backup-and-restore.md](backup-and-restore.md)
