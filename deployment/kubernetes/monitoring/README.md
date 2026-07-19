# Kubernetes monitoring (optional)

Local/dev observability runs via `deployment/docker-compose-infra.yml` (Prometheus, Grafana, Loki, Tempo, Alertmanager, OTel Collector).

For cluster installs, prefer the upstream Helm charts:

| Component | Chart |
|-----------|-------|
| kube-prometheus-stack | `prometheus-community/kube-prometheus-stack` |
| Loki | `grafana/loki-stack` or `grafana/loki` |
| Tempo | `grafana/tempo` |
| OpenTelemetry Operator | `open-telemetry/opentelemetry-operator` |

Point platform services at the in-cluster OTLP endpoint (`http://otel-collector:4318`) using ConfigMap `TRACING` / Spring observability settings already provided by `common-observability`.
