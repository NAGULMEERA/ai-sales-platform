# Deployment

Runtime deployment assets for local development and Kubernetes.

```
deployment/
├── docker-compose.yml           # Compose entrypoint (includes infra + services)
├── docker-compose-infra.yml     # PostgreSQL, Redis, Kafka, Zipkin, Prometheus
├── docker-compose-services.yml  # Gateway, registry, config, core services
└── kubernetes/                  # K8s manifests per service
```

## Local

```bash
docker network create aisales-network
docker compose -f deployment/docker-compose-infra.yml up -d
docker compose -f deployment/docker-compose-services.yml up -d --build
```

## Kubernetes

```bash
kubectl apply -f deployment/kubernetes/
```
