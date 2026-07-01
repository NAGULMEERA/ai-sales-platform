# Deployment & Infrastructure Strategy

## Environments

| Environment | Purpose | Data | Scale | Access |
|-------------|---------|------|-------|--------|
| Local | Developer testing | Mock / Docker | 1 container | Developer |
| Dev | Integration | Synthetic | 1 replica | Internal |
| Staging | Pre-production | Sanitized prod | 3 replicas | Internal + select customers |
| Canary | Gradual rollout | Production | 1% traffic | Controlled |
| Production | Live | Production | Auto-scale | All customers |

---

## Deployment Patterns

| Pattern | Use |
|---------|-----|
| **Rolling update** | Default for stateless services |
| **Blue-green** | Zero-downtime major releases |
| **Canary** | 1% → 10% → 50% → 100% traffic shift |
| **Feature flags** | Toggle without redeploy |

**Rollback target:** ≤ 2 minutes via previous image tag.

---

## Infrastructure Components

```
deployment/
├── docker-compose*.yml     # Local stack
└── kubernetes/             # Per-service manifests

infrastructure/
├── api-gateway/
├── service-registry/
├── config-server/
└── monitoring/             # Prometheus, Grafana, Alertmanager

automation/
├── jenkins/
└── workflows-reference/
```

---

## Kubernetes Standards

Every service Deployment includes:

- `livenessProbe`: `/actuator/health/liveness`
- `readinessProbe`: `/actuator/health/readiness`
- Resource requests/limits (512Mi–1Gi memory baseline)
- `SPRING_PROFILES_ACTIVE=prod`
- Secrets via `secretKeyRef` (never in ConfigMap)

Example manifest: `deployment/kubernetes/lead-service.yml`

---

## CI/CD

| Stage | Tool | Path |
|-------|------|------|
| Build & test | GitHub Actions | `.github/workflows/ci.yml` |
| Docker images | GitHub Actions | `.github/workflows/docker-build.yml` |
| Deploy (future) | GitHub Actions / Jenkins | `automation/` |

---

## Related

- [deployment-guide.md](../11-devops/deployment-guide.md)
- [disaster-recovery-plan.md](../12-operations/disaster-recovery-plan.md)
