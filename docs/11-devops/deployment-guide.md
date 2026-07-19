# Deployment Guide

Grounded in `deployment/` compose files, `deployment/kubernetes/`, and `scripts/`.

## Prerequisites

- Docker + Docker Compose
- JDK 21 and Maven wrapper (`backend/mvnw`)
- Optional: kubectl for Kubernetes

## Local infrastructure

```bash
docker network create aisales-network
docker compose -f deployment/docker-compose-infra.yml up -d
```

Infra stack (from compose) includes:

| Component | Host ports (local) |
|-----------|--------------------|
| PostgreSQL (pgvector/pg16) | `5433→5432` |
| Redis | `6379` |
| Kafka | `9092` |
| Kafka UI | `8085` |
| Mailpit SMTP/UI | `1025` / `8025` |
| OpenTelemetry collector | (compose) |
| Prometheus / Alertmanager | (compose; see ops docs) |

Databases are initialized via `deployment/postgres/init-databases.sql`.

## Build

```bash
# From repo root
./scripts/build-all.sh
# or
cd backend && ./mvnw clean install -DskipTests
```

Single service:

```bash
./scripts/build-service.sh services/identity-service
```

## Local services

```bash
docker compose -f deployment/docker-compose-services.yml up -d --build
# or umbrella
docker compose -f deployment/docker-compose.yml up -d
```

Helper: `./scripts/deploy-local.sh` (if present in `scripts/`).

## Kubernetes

```bash
kubectl apply -f deployment/kubernetes/namespace.yml
kubectl apply -f deployment/kubernetes/configmap.yml
kubectl apply -f deployment/kubernetes/secrets.yml
kubectl apply -f deployment/kubernetes/
```

Production hardening notes:

- [production-deployment.md](production-deployment.md) — full production runbook (Helm, scanning, observability)
- [blue-green-deployment.md](blue-green-deployment.md)
- [kubernetes-production-checklist.md](kubernetes-production-checklist.md)
- [rolling-deploy-and-flyway.md](rolling-deploy-and-flyway.md)
- [production-readiness-review.md](production-readiness-review.md)

Deployments use health probes (`/actuator/health/liveness`, readiness), startup probes, RollingUpdate strategy, HPA/PDB/NetworkPolicy (see `deployment/kubernetes/`), and graceful shutdown from shared prod/observability YAML.

Helm (preferred for prod):

```bash
helm upgrade --install aisales deployment/helm/aisales-platform \
  --namespace aisales --create-namespace \
  --set global.imageTag=v1.0.0
```

## Gateway entry

| Component | Typical local port |
|-----------|--------------------|
| API Gateway | 8080 |
| Eureka | 8761 |
| Config Server | 8888 |

All external `/api/v1/**` traffic should enter through the gateway.

## Configuration

- Shared platform YAML under config/resources used by services (`application-observability.yml`, `application-prod.yml` patterns)
- Secrets via env / K8s secrets (JWT keys, DB passwords, provider keys) — never commit production secrets
- See [ADR-011 secrets](../15-adr/adr-011-secrets-management.md)

## Flyway

Each service owns `src/main/resources/db/migration/`. Apply by starting the service (Flyway on boot) or via documented rolling deploy procedure. **Never edit applied migrations.**

## Related

- [production-deployment.md](production-deployment.md)
- [blue-green-deployment.md](blue-green-deployment.md)
- [deployment-strategy.md](deployment-strategy.md)
- [../12-operations/operational-guide.md](../12-operations/operational-guide.md)
- [../12-operations/disaster-recovery-guide.md](../12-operations/disaster-recovery-guide.md)
- [../14-developer-guide/developer-onboarding.md](../14-developer-guide/developer-onboarding.md)
