# Alerting and SLOs

## Prometheus

- Config: `infrastructure/monitoring/prometheus/prometheus.yml`
- Rules: `infrastructure/monitoring/prometheus/alerts.yml`
- Alertmanager: `infrastructure/monitoring/alertmanager/alertmanager.yml`
- Local stack: `deployment/docker-compose-infra.yml` (prometheus + alertmanager + grafana)

## Alert catalog

| Alert | Severity | Meaning | First action |
|-------|----------|---------|--------------|
| ServiceDown | critical | Scrape target down | Check pod/process + readiness |
| HighHttp5xxRate | high | >5% 5xx for 5m | Logs, dependency health, recent deploy |
| HighHttpLatencyP99 | medium | p99 > 2s for 10m | Slow queries, AI provider, pool saturation |
| CircuitBreakerOpen | high | Outbound CB open | Upstream provider / Kafka / SMTP |
| OutboxLagHigh | high | Outbox backlog | Publisher errors, Kafka availability |
| JvmHeapPressure | medium | Heap > 90% | Heap dump / scale memory |

## Platform SLOs (initial targets)

| Capability | SLO | Notes |
|------------|-----|-------|
| API availability (gateway + identity + lead) | 99.9% monthly | Exclude planned maintenance |
| Auth login p99 | < 500 ms | Excludes IdP latency |
| Lead create p99 | < 1 s | Excludes AI qualification |
| AI execute p99 | < 8 s | Provider-dependent |
| Outbox publish lag | < 30 s p99 | Steady state |

Tune thresholds after baseline traffic is measured.

## Runbooks

- Incidents: [incident-response-runbook.md](incident-response-runbook.md)
- Eventing: [eventing-ops-matrix.md](eventing-ops-matrix.md)
