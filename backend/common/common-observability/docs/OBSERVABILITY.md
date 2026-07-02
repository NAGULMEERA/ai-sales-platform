# Platform Observability — Phase 2

## Tracing (OpenTelemetry)

The platform uses **Micrometer Tracing + OpenTelemetry OTLP** (Brave/Zipkin removed in Phase 2).

### Enable

Import shared defaults (via `common-starter` classpath):

```yaml
spring:
  config:
    import: optional:classpath:platform/application-observability.yml
```

### Export traces

Run OTel Collector (`infrastructure/monitoring/otel-collector-config.yaml`) or point to your backend:

```bash
export OTEL_EXPORTER_OTLP_ENDPOINT=http://localhost:4318/v1/traces
export ENVIRONMENT=local
```

### Log correlation

Log pattern includes `traceId`, `spanId`, and `correlation_id`:

```
%5p [service-name,traceId,spanId,correlation_id]
```

---

## Business metrics

Use `PlatformMetrics` + `MetricNames` for consistent KPIs:

```java
@Component
@RequiredArgsConstructor
public class LeadMetrics {
    private final PlatformMetrics platformMetrics;

    public void recordLeadCreated(String tenantId) {
        platformMetrics.incrementForTenant(MetricNames.LEAD_CREATED, tenantId);
    }
}
```

All metrics receive common tags: `application`, `service`, `environment`.

---

## Kafka trace propagation

W3C trace context is injected on outbox dispatch and continued on consume via `IntegrationEventListener`.

Pipeline: REST span → outbox publish → Kafka headers → consumer span → logs.

---

## Prometheus

Expose `/actuator/prometheus`. Scrape config: `infrastructure/monitoring/prometheus/prometheus.yml`.

Grafana dashboards:
- `platform-services-dashboard.json` — JVM + HTTP + business counters
- `business-metrics-dashboard.json` — KPI panels

---

## Related

- Rule 08 — Observability Governance
- `docs/03-architecture/platform-infrastructure-epic.md`
