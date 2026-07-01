# Deployment Guide

## Local Development

```bash
# Start infrastructure
docker network create aisales-network
docker compose -f deployment/docker-compose-infra.yml up -d

# Build all modules
cd backend && ./mvnw clean install -DskipTests

# Start services
docker compose -f deployment/docker-compose-services.yml up -d
```

## Kubernetes

```bash
kubectl apply -f deployment/kubernetes/namespace.yml
kubectl apply -f deployment/kubernetes/configmap.yml
kubectl apply -f deployment/kubernetes/secrets.yml
kubectl apply -f deployment/kubernetes/
```

## Ports

| Service | Port |
|---------|------|
| API Gateway | 8080 |
| Identity Service | 8081 |
| Tenant Service | 8082 |
| Service Registry | 8761 |
| Config Server | 8888 |
