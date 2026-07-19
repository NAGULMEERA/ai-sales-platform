# Deployment

Runtime deployment assets for local development and Kubernetes.

```
deployment/
├── docker-compose.yml              # Umbrella (infra + services)
├── docker-compose-infra.yml        # Data plane + observability
├── docker-compose-services.yml     # Gateway, registry, config, domain services
├── postgres/                       # DB bootstrap SQL
├── kubernetes/                     # Raw manifests (probes, HPA, PDB, NetPol, Ingress/TLS)
└── helm/aisales-platform/          # Production Helm chart (rolling + blue/green)
```

## Local

```bash
docker network create aisales-network || true
docker compose -f deployment/docker-compose-infra.yml up -d
docker compose -f deployment/docker-compose-services.yml up -d --build
```

Stub services (appointment/audit) are behind Compose profile `full`.

## Images

Multi-stage Dockerfiles live next to each module. Build from **repository root**:

```bash
./scripts/docker-build-all.sh
docker build -f backend/services/lead-service/Dockerfile -t aisales/lead-service:latest .
```

## Kubernetes

```bash
kubectl apply -f deployment/kubernetes/namespace.yml
kubectl apply -f deployment/kubernetes/configmap.yml
kubectl apply -f deployment/kubernetes/secrets.yml
kubectl apply -f deployment/kubernetes/
```

Or Helm:

```bash
helm upgrade --install aisales deployment/helm/aisales-platform \
  --namespace aisales --create-namespace \
  --set global.imageTag=v1.0.0
```

## Docs

- [Production deployment runbook](../docs/11-devops/production-deployment.md)
- [Blue/green](../docs/11-devops/blue-green-deployment.md)
- [DR guide](../docs/12-operations/disaster-recovery-guide.md)
