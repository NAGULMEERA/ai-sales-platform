# ADR-001: Microservices Architecture

**Status**: Accepted

## Context

The platform requires independent scaling, team ownership, and technology flexibility across sales domains.

## Decision

Adopt a microservices architecture with:
- Spring Boot 3.2 / Java 21
- Netflix Eureka for service discovery
- Spring Cloud Gateway as API edge
- PostgreSQL per service (database-per-service)
- Kafka for domain events

## Consequences

- Higher operational complexity mitigated by Docker Compose (local) and Kubernetes (prod)
- Consistent cross-cutting concerns handled by common modules
