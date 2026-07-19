# Production Readiness Review

Date focus: platform shared patterns (config, K8s, health, resilience, observability, backup).

## Scorecard

| Area | Status | Notes |
|------|--------|-------|
| Configuration / profiles | Improved | Shared `platform/application-prod.yml`; per-service prod YAML expanding |
| Docker / Compose | Improved | Identity/lead images: Java 21, non-root, container JVM flags; Alertmanager wired |
| Kubernetes readiness | Improved | YAML corruption fixed; probes/resources/preStop/terminationGrace applied |
| Health / liveness / readiness | Ready | Spring health groups + K8s probe paths aligned |
| Graceful shutdown | Ready | `server.shutdown=graceful` + 30s phase timeout |
| Retry / circuit breakers | Improved | Shared Resilience4j defaults for outbox/SMTP |
| Outbox / Inbox / DLQ | Adequate | `common-events` patterns; see eventing ops matrix |
| Backup / recovery | Ready | Scripts + docs; DR plan links restored |
| Observability | Improved | Metrics scrape expanded; Prometheus alert rules added |
| Secrets | Adequate | Vault/ESO pattern; prod profiles require env secrets |
| Upgrade safety | Documented | Rolling deploy + Flyway guide |
| Dependency management | Partial | BOM + Dependabot; more Docker dirs tracked |

## Implemented in this review

1. Fixed invalid K8s Deployment YAML (JWKS value concatenated with next key).
2. Hardened Deployments: startup/liveness/readiness, resources, preStop, `terminationGracePeriodSeconds`.
3. Shared graceful shutdown + health probe groups in `platform/application-observability.yml`.
4. Shared prod profile defaults in `platform/application-prod.yml`.
5. Prometheus `alerts.yml` + Alertmanager compose service.
6. PostgreSQL backup/restore scripts.
7. Operations and DevOps documentation set under `docs/12-operations` and `docs/11-devops`.
8. Identity/Lead Dockerfiles aligned to Java 21 non-root.

## Remaining follow-ups

1. Pin production images by digest; add HPA/PDB/NetworkPolicy.
2. Provision managed Postgres/Kafka/Redis (or K8s data-plane charts) — not in-repo today.
3. Replace staging deploy workflow stubs with real `kubectl`/Helm apply.
4. Roll non-root Dockerfile pattern to remaining services.
5. Baseline SLOs with real traffic, then tighten alert thresholds.
6. Enable `JWT_REQUIRE_ISS_AUD=true` after token rollover (already default in prod platform YAML).

## Operator entry points

- Checklist: [kubernetes-production-checklist.md](kubernetes-production-checklist.md)
- Rolling upgrades: [rolling-deploy-and-flyway.md](rolling-deploy-and-flyway.md)
- Ops index: [../12-operations/README.md](../12-operations/README.md)
- Harden script: `scripts/k8s-harden-deployments.ps1`
