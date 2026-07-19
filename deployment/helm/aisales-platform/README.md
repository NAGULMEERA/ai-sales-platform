# aisales-platform Helm chart

Deploys AI Sales Platform microservices with probes, resources, HPA, PDB, NetworkPolicy, Ingress/TLS, and optional blue/green traffic switching.

## Prerequisites

- Kubernetes 1.27+
- Helm 3.14+
- Namespace `aisales` (`kubectl apply -f ../../kubernetes/namespace.yml`)
- Secret `aisales-secrets` (see `../../kubernetes/secrets.yml` or External Secrets)
- Managed/external Postgres, Redis, Kafka reachable from the cluster

## Install (rolling)

```bash
helm upgrade --install aisales ./deployment/helm/aisales-platform \
  --namespace aisales \
  --create-namespace \
  --set global.imageTag=v1.2.3
```

## Blue/Green

```bash
# Enable dual-color Deployments
helm upgrade --install aisales ./deployment/helm/aisales-platform \
  -f values-bluegreen.yaml \
  --set global.imageTag=v1.2.3 \
  --set global.activeColor=blue

# Deploy new build onto inactive color only
helm upgrade aisales ./deployment/helm/aisales-platform \
  -f values-bluegreen.yaml \
  --set global.activeColor=blue \
  --set global.colorImageTags.blue=v1.2.3 \
  --set global.colorImageTags.green=v1.2.4

# After smoke tests, flip traffic
helm upgrade aisales ./deployment/helm/aisales-platform \
  -f values-bluegreen.yaml \
  --set global.activeColor=green \
  --set global.colorImageTags.blue=v1.2.3 \
  --set global.colorImageTags.green=v1.2.4
```

## Flyway

By default each service runs Flyway on boot (`spring.flyway`). Set `flyway.enabled=true` only when you mount migration ConfigMaps for gated cutovers.

## Raw manifests

Plain YAML under `../../kubernetes/` remains supported for clusters that do not use Helm.
