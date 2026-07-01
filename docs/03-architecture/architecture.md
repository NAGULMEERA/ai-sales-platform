# Architecture Overview

AI Sales Platform is a multi-tenant microservices system built with Spring Boot 3.2, Java 21, and Spring Cloud.

## Components

| Layer | Modules |
|-------|---------|
| Common | core, exception, security, events, contracts, observability, testing, starter |
| Infrastructure | Eureka registry, Config Server, API Gateway, Discovery |
| Services | identity, tenant (+ 11 domain services) |

## Communication

- **Sync**: REST via API Gateway + OpenFeign clients
- **Async**: Kafka events with outbox pattern support
- **Discovery**: Netflix Eureka
- **Config**: Spring Cloud Config (native profile)

## Multi-Tenancy

Tenant context is propagated via JWT claims and `X-Tenant-Id` header. All domain services enforce tenant isolation at the data layer.

## Observability

- Correlation IDs (`X-Correlation-Id`) across gateway and services
- Micrometer metrics + Prometheus scraping
- Zipkin distributed tracing

## Security

JWT-based authentication (identity-service) with refresh tokens. Resource server validation via common-security module.
