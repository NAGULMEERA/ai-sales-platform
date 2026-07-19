# Kubernetes Production Checklist

Use before promoting manifests under `deployment/kubernetes/`.

## Required on every Deployment

- [ ] Valid YAML (env values on their own line)
- [ ] `SPRING_PROFILES_ACTIVE=prod`
- [ ] Secrets via `secretKeyRef` / External Secrets (never inline)
- [ ] `terminationGracePeriodSeconds` ≥ 45 (matches graceful shutdown)
- [ ] `startupProbe` → `/actuator/health/readiness`
- [ ] `livenessProbe` → `/actuator/health/liveness`
- [ ] `readinessProbe` → `/actuator/health/readiness`
- [ ] `resources.requests` and `resources.limits`
- [ ] `lifecycle.preStop` sleep (allow endpoint drain)
- [ ] Matching `Service` selector/ports

## Platform helpers

- Harden script: `scripts/k8s-harden-deployments.ps1`
- Shared Spring defaults: `platform/application-observability.yml`, `platform/application-prod.yml`

## Recommended (next)

- [ ] HPA for gateway, identity, lead, AI
- [ ] PodDisruptionBudget (minAvailable 1) for replicas ≥ 2
- [ ] NetworkPolicies (deny by default)
- [ ] Pin image digests (no `:latest` in prod)
- [ ] Separate data-plane charts for Postgres/Kafka/Redis (or managed services)

## Probe contract

Spring Boot exposes groups when `management.endpoint.health.probes.enabled=true` (platform observability YAML):

| Probe | Path | Failure meaning |
|-------|------|-----------------|
| Startup / Readiness | `/actuator/health/readiness` | Do not send traffic (DB not ready, etc.) |
| Liveness | `/actuator/health/liveness` | Restart container |
