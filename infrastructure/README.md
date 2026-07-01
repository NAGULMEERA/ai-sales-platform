# Infrastructure

Platform infrastructure components and observability stack.

```
infrastructure/
├── api-gateway/        # Spring Cloud Gateway
├── config-server/      # Spring Cloud Config
├── discovery-service/  # Service discovery client utilities
├── service-registry/   # Netflix Eureka
└── monitoring/         # Prometheus, Grafana, Alertmanager
```

These modules are Maven submodules referenced from `backend/pom.xml` via `../infrastructure/*`.
