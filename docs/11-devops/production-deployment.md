# Production Deployment Runbook

Enterprise deployment guide for the AI Sales Platform. Extends [deployment-guide.md](deployment-guide.md) with Helm, blue/green, scanning, and observability.

## Inventory (reuse first)

| Asset | Location | Status |
|-------|----------|--------|
| Multi-stage Dockerfiles | `backend/services/*/Dockerfile`, `infrastructure/*/Dockerfile` | Ready |
| Local infra compose | `deployment/docker-compose-infra.yml` | Ready (Postgres, Redis, Kafka, Prometheus, Grafana, Loki, Tempo, Alertmanager, OTel) |
| Local services compose | `deployment/docker-compose-services.yml` | Ready (all services; stubs behind profile `full`) |
| Raw Kubernetes | `deployment/kubernetes/` | Ready (+ HPA, PDB, NetworkPolicy, TLS Ingress) |
| Helm chart | `deployment/helm/aisales-platform/` | Ready (rolling + blue/green) |
| CI build/test | `.github/workflows/ci.yml`, `build.yml`, `test.yml` | Ready |
| Image build + Trivy | `.github/workflows/docker-build.yml` | Ready |
| Dependency scan | `.github/workflows/security-scan.yml` | Ready |
| Deploy staging/prod | `.github/workflows/deploy-staging.yml`, `deploy-prod.yml` | Ready (requires kubeconfig secrets) |
| Flyway gated job | `.github/workflows/flyway-migrate.yml` | Ready (optional; default is migrate-on-boot) |
| Backup scripts | `scripts/backup-postgres.*`, `restore-postgres.sh` | Ready |
| DR docs | `docs/12-operations/disaster-recovery-*.md` | Ready |

## Local development

```bash
docker network create aisales-network || true
docker compose -f deployment/docker-compose-infra.yml up -d
docker compose -f deployment/docker-compose-services.yml up -d --build
# Include scaffold services:
# docker compose -f deployment/docker-compose-services.yml --profile full up -d --build
```

Observability URLs:

| UI | URL |
|----|-----|
| Grafana | http://localhost:3000 (admin/admin local only) |
| Prometheus | http://localhost:9090 |
| Alertmanager | http://localhost:9093 |
| Loki | http://localhost:3100 |
| Tempo | http://localhost:3200 |
| Kafka UI | http://localhost:8085 |
| Mailpit | http://localhost:8025 |

## Container images

Build all images (repo root context):

```bash
./scripts/docker-build-all.sh
# or IMAGE_TAG=v1.2.3 ./scripts/docker-build-all.sh
```

Regenerate Dockerfiles if the template changes:

```powershell
powershell -File ./scripts/generate-service-dockerfiles.ps1
```

## Kubernetes (raw manifests)

```bash
kubectl apply -f deployment/kubernetes/namespace.yml
kubectl apply -f deployment/kubernetes/configmap.yml
# Prefer External Secrets in prod — see deployment/kubernetes/external-secrets/
kubectl apply -f deployment/kubernetes/secrets.yml
kubectl apply -f deployment/kubernetes/
kubectl apply -f deployment/kubernetes/hpa.yml
kubectl apply -f deployment/kubernetes/pdb.yml
kubectl apply -f deployment/kubernetes/network-policies.yml
```

Create TLS secret before Ingress serves HTTPS:

```bash
kubectl -n aisales create secret tls aisales-tls --cert=api.crt --key=api.key
```

Every Deployment includes:

- RollingUpdate (`maxUnavailable: 0`, `maxSurge: 1`)
- startup / liveness / readiness probes
- CPU/memory requests and limits
- `terminationGracePeriodSeconds: 45` + `preStop` sleep

## Helm

```bash
helm upgrade --install aisales deployment/helm/aisales-platform \
  --namespace aisales --create-namespace \
  --set global.imageTag=v1.2.3
```

### Blue/Green

1. Install with `-f values-bluegreen.yaml` and `global.activeColor=blue`.
2. Deploy the new tag onto the inactive color.
3. Smoke-test inactive pods.
4. Flip `global.activeColor` to the new color (Service selector switches traffic).

Details: [deployment/helm/aisales-platform/README.md](../../deployment/helm/aisales-platform/README.md).

## CI/CD secrets

| Secret | Used by |
|--------|---------|
| `KUBE_CONFIG_STAGING` | deploy-staging (base64 kubeconfig) |
| `KUBE_CONFIG_PRODUCTION` | deploy-prod |
| `IMAGE_REGISTRY` | Helm image registry prefix |
| `FLYWAY_JDBC_URL_BASE` | Optional gated Flyway workflow |
| `FLYWAY_USER` / `FLYWAY_PASSWORD` | Optional gated Flyway workflow |

## Flyway strategy

1. **Default:** each service runs Flyway on startup (backward-compatible, already implemented).
2. **Gated cutover:** `.github/workflows/flyway-migrate.yml` or Helm `flyway.enabled=true` with migration ConfigMaps.
3. **Never** edit applied migrations — add a new versioned script.

See [rolling-deploy-and-flyway.md](rolling-deploy-and-flyway.md).

## Security scanning

- **Container:** Trivy in `docker-build.yml` (fails on CRITICAL/HIGH).
- **Dependencies:** OWASP Dependency-Check in `security-scan.yml`.
- **Filesystem/config:** Trivy fs scan on PRs.
- **Dependabot:** `.github/dependabot.yml` (Maven + Docker + Actions).

## Backup and disaster recovery

- Daily logical dumps: `scripts/backup-postgres.sh`
- Restore: `scripts/restore-postgres.sh`
- Objectives and drill cadence: [../12-operations/disaster-recovery-guide.md](../12-operations/disaster-recovery-guide.md)

## Production checklist (abbreviated)

- [ ] Managed Postgres (HA) + PITR / WAL archive
- [ ] Managed Kafka + Redis (or hardened operators)
- [ ] External Secrets / Vault wired; placeholder `secrets.yml` not used with real values
- [ ] TLS secret or cert-manager Certificate
- [ ] NetworkPolicies reviewed for your CNI
- [ ] HPA metrics-server installed
- [ ] Image digests pinned (`global.imageTag` immutable)
- [ ] Grafana/Alertmanager notification channels configured
- [ ] Quarterly DR drill completed

## Related

- [kubernetes-production-checklist.md](kubernetes-production-checklist.md)
- [deployment-strategy.md](deployment-strategy.md)
- [production-readiness-review.md](production-readiness-review.md)
- [../12-operations/backup-and-restore.md](../12-operations/backup-and-restore.md)
