# Developer Onboarding Guide

Get a new engineer productive on the **existing** codebase.

## 1. Read first (in order)

1. [../01-vision/README.md](../01-vision/README.md) — product intent  
2. [../03-architecture/architecture.md](../03-architecture/architecture.md) + [c4-containers.md](../03-architecture/c4-containers.md)  
3. [../07-microservices/service-catalog.md](../07-microservices/service-catalog.md)  
4. [../15-adr/README.md](../15-adr/README.md) — especially freeze ADR-030  
5. [coding-standards.md](coding-standards.md) + [development-guide.md](development-guide.md)

## 2. Tooling

| Tool | Version / note |
|------|----------------|
| JDK | 21+ |
| Maven | Wrapper at `backend/mvnw` / `backend/mvnw.cmd` |
| Docker | Required for infra + Testcontainers |
| IDE | Import `backend` Maven reactor |

Windows: use PowerShell; chain commands with `;` not `&&` if needed.

## 3. First local run

```bash
docker network create aisales-network
docker compose -f deployment/docker-compose-infra.yml up -d
cd backend && ./mvnw clean install -DskipTests
# start identity (example)
./mvnw -pl services/identity-service spring-boot:run
```

Or use compose services / `scripts/deploy-local.sh` / `scripts/build-all.sh` as documented in [deployment-guide.md](../11-devops/deployment-guide.md).

Mailpit UI (local email): `http://localhost:8025`.

## 4. Repository map

```
backend/common/*          shared libraries (starter, security, events, contracts)
backend/services/*        one deployable per bounded context
infrastructure/*          gateway, registry, config
deployment/*              compose + kubernetes
docs/*                    this documentation tree
scripts/*                 build, test, backup helpers
.cursor/rules/*           architecture governance rules for agents
```

## 5. How to add a feature (mandatory ownership questions)

Before coding, answer: which **business capability**, **bounded context**, **service**, **aggregate** owns it? Search for existing DTOs/events/APIs. Prefer extension over duplication (see Rule 01 in `.cursor/rules`).

Typical vertical slice:

1. Contract DTO in `common-contracts` (if public)  
2. Flyway migration in owning service  
3. Domain entity + application service  
4. REST controller  
5. Outbox event if other services must react  
6. Tests (unit + Testcontainers where persistence matters)

## 6. Testing

```bash
./scripts/run-tests.sh
# or targeted
cd backend && ./mvnw -pl services/lead-service -am test
```

Harness notes: `backend/common/common-testing/docs/INTEGRATION_TEST_HARNESS.md`.  
Coverage gaps: [../13-quality/coverage-matrix.md](../13-quality/coverage-matrix.md).

## 7. Security basics

- Never accept `tenantId` from request body for authorization; use JWT / `TenantContext`.
- Do not call LLM SDKs outside ai-service.
- Do not log tokens, passwords, or raw sensitive prompts.

## 8. Useful deep dives

| Topic | Doc |
|-------|-----|
| Events | [../17-reference/event-catalog.md](../17-reference/event-catalog.md) |
| Workflows | [../07-microservices/workflow.md](../07-microservices/workflow.md) |
| AI / RAG | [../06-ai-platform/ai-architecture.md](../06-ai-platform/ai-architecture.md) |
| Sequences | [../03-architecture/sequence-diagrams.md](../03-architecture/sequence-diagrams.md) |
| APIs | [../08-api/api-surface.md](../08-api/api-surface.md) |

## 9. Definition of done (engineering)

- Ownership clear; no cross-DB joins  
- Tenant isolation enforced  
- Tests for business behavior  
- Events versioned / `EVENT_TYPE` used  
- Docs updated if public contract or ops procedure changed  
