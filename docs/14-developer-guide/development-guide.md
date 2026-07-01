# Development Guide

## Prerequisites

- JDK 21+
- Docker & Docker Compose
- Maven 3.9+ (or use `backend/mvnw`)

## Quick Start

```bash
# Build all modules
./scripts/build-all.sh

# Start infrastructure + core services
docker network create aisales-network
docker compose -f deployment/docker-compose-infra.yml up -d
docker compose -f deployment/docker-compose-services.yml up -d --build
```

## Running a Single Service

```bash
./scripts/build-service.sh services/identity-service
java -jar backend/services/identity-service/target/identity-service-*.jar
```

## Code Style

- Google Java Format via Spotless: `cd backend && ./mvnw spotless:apply`
- Checkstyle: `cd backend && ./mvnw checkstyle:check`

## Module Dependencies

Services depend on `common-starter` which auto-configures web, security, exception handling, and observability.

## Database Migrations

Flyway migrations live in `backend/services/{service}/src/main/resources/db/migration/`.

## Testing

```bash
./scripts/run-tests.sh
```

Integration tests use Testcontainers via `common-testing` module.
