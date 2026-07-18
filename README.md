# AI Sales Platform

Multi-tenant, AI-powered sales platform built as a Spring Boot 3.2 / Java 21 microservices monorepo.

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌─────────────────────────────┐
│ API Gateway │────▶│   Eureka     │────▶│  Microservices (18)       │
│   :8080     │     │  Registry    │     │  identity, tenant, lead...  │
└─────────────┘     │   :8761      │     └─────────────────────────────┘
                    └──────────────┘
                           │
                    ┌──────┴───────┐
                    │ Config Server│
                    │    :8888     │
                    └──────────────┘
```

## Project Structure

```
ai-sales-platform/
├── docs/                   # Platform documentation (vision → templates)
├── backend/                # Java microservices monorepo (Maven)
│   ├── common/             # Shared libraries
│   ├── services/           # Domain microservices (18 bounded contexts)
│   ├── plugins/            # Plugin SDK + industry plugins
│   └── database/           # Flyway lab migrations & split maps
├── frontend/               # Web and mobile clients (placeholder)
├── infrastructure/         # Gateway, Eureka, Config Server, monitoring
├── deployment/             # Docker Compose & Kubernetes manifests
├── automation/             # Jenkins & CI workflow references
├── scripts/                # Build, deploy, scaffold scripts
├── tools/                  # Developer utilities
├── .cursor/                # Cursor rules, agents, knowledge
├── .augment/               # Augment IDE configuration
└── .github/workflows/      # GitHub Actions CI/CD
```

Each service follows DDD layers:

```
com.aisales.{context}/
├── api/            # controllers, request/response DTOs
├── application/    # use cases, mappers
├── domain/         # entities, aggregates, domain events
└── infrastructure/ # persistence, messaging, configuration
```

## Tech Stack

| Category | Technology |
|----------|------------|
| Runtime | Java 21, Spring Boot 3.2.5 |
| Cloud | Spring Cloud 2023.0.1 |
| Database | PostgreSQL 16, Flyway |
| Messaging | Apache Kafka |
| Discovery | Netflix Eureka |
| Gateway | Spring Cloud Gateway |
| Security | JWT (jjwt 0.12) |
| Testing | JUnit 5, Testcontainers, ArchUnit |
| Observability | Micrometer, Zipkin, Prometheus |
| Docs | SpringDoc OpenAPI 2.5 |

## Quick Start

### Prerequisites

- JDK 21+
- Docker & Docker Compose
- Maven 3.9+ (or use `backend/mvnw`)

### Build

```bash
# Windows
cd backend
mvnw.cmd clean install -DskipTests

# Linux/macOS
cd backend
./mvnw clean install -DskipTests
```

### Run Locally

```bash
docker network create aisales-network
docker compose -f deployment/docker-compose-infra.yml up -d
docker compose -f deployment/docker-compose-services.yml up -d --build
```

## Services

| Service | Port | Status |
|---------|------|--------|
| identity-service | 8081 | Implemented |
| tenant-service | 8082 | Implemented |
| lead-service | 8083 | Scaffolded |
| customer-service | 8084 | Scaffolded |
| catalog-service | 8085 | Scaffolded |
| conversation-service | 8086 | Scaffolded |
| appointment-service | 8087 | Scaffolded |
| ai-service | 8088 | Prompts + AI Gateway + KB metadata |
| workflow-service | 8089 | Scaffolded |
| notification-service | 8090 | Scaffolded |
| billing-service | 8091 | Scaffolded |
| integration-service | 8092 | Scaffolded |
| analytics-service | 8093 | Scaffolded |
| search-service | 8094 | Scaffolded |
| media-service | 8095 | Scaffolded |
| audit-service | 8096 | Scaffolded |
| deal-service | 8097 | Opportunity + Quote |
| marketplace-service | 8098 | Plugin registry (catalog + enable/disable) |

## Development

```bash
cd backend && ./mvnw spotless:apply
./scripts/run-tests.sh
./scripts/scaffold-service-structure.ps1   # regenerate service DDD scaffold
```

## CI/CD

- **GitHub Actions**: `.github/workflows/`
- **Jenkins**: `automation/jenkins/Jenkinsfile`

## Documentation

See [docs/README.md](docs/README.md) for the full documentation index.

## License

Proprietary — AI Sales Platform Team. See [LICENSE](LICENSE).
