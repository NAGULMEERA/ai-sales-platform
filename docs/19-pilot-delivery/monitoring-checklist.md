# Monitoring Checklist

Before pilot traffic. Detail SLOs: [alerting-and-slos.md](../12-operations/alerting-and-slos.md). Telemetry: `common-observability`.

## Metrics (Prometheus)

- [ ] All pilot services scrape `/actuator/prometheus`
- [ ] Gateway: request rate, 4xx/5xx, rate-limit 429s
- [ ] Hikari: active/pending/timeouts (`HIKARI_*` env tuned)
- [ ] Kafka: consumer lag, DLT publish rate
- [ ] Outbox: pending / failed publish counters (per service)
- [ ] AI: prompt/completion tokens, provider errors, quota denials
- [ ] Business: leads created, quotes created, workflows completed (where instrumented)

## Logs

- [ ] Structured JSON with `correlation_id`, `trace_id`, `tenant_id`
- [ ] No passwords/tokens/PII in clear text (redactor in place)
- [ ] Retention set for pilot (app vs audit vs security)

## Traces

- [ ] OTLP exporter endpoint set (`OTEL_EXPORTER_OTLP_ENDPOINT`)
- [ ] Sample rate appropriate for pilot load
- [ ] End-to-end: Gateway → service → Kafka consumer span continuity spot-checked

## Alerts (minimum for pilot)

- [ ] High error rate (5xx) per service
- [ ] Auth failure spike
- [ ] Kafka lag above threshold
- [ ] Database connection pool exhaustion
- [ ] AI provider failure / circuit open
- [ ] Disk / PVC capacity (media, Postgres)

## Dashboards

- [ ] Service health board
- [ ] Kafka / outbox board
- [ ] AI cost & latency board
- [ ] Business KPI board (leads, quotes, AI conversations)

## Sign-off

| Check | Owner | Done |
|-------|-------|------|
| Alerts routed to on-call channel | DevOps | [ ] |
| Runbook links in Alertmanager annotations | DevOps | [ ] |
| Pilot CS has status page / contact | Delivery | [ ] |
