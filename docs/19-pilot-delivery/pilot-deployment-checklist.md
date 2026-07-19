# Pilot Deployment Checklist

Use for the first 1–3 design-partner tenants. Complete in order. Checkboxes are delivery gates.

## 0. Preconditions

- [ ] Environment labeled `pilot` / `staging` (not shared with uncontrolled prod tenants)
- [ ] Secrets from Vault/ESO only — no committed PEMs in runtime (ADR-011)
- [ ] JDK 21 images built via multi-stage Dockerfiles / Helm chart
- [ ] Postgres (pgvector), Redis, Kafka healthy
- [ ] Gateway routes to all required services

Refs: [deployment-guide.md](../11-devops/deployment-guide.md), [production-deployment.md](../11-devops/production-deployment.md)

## 1. Infrastructure

- [ ] `deployment/postgres/init-databases.sql` (or equiv) created per-service DBs
- [ ] Flyway applied for identity, tenant, lead, catalog, deal, ai, marketplace, billing, search, analytics, workflow, notification, media, conversation, integration
- [ ] Redis reachable from gateway (rate limits) and identity (RBAC cache if enabled)
- [ ] Kafka topics / consumer groups created; DLTs observed empty at idle
- [ ] OTLP → Tempo/collector; Prometheus scrapes `/actuator/prometheus`
- [ ] Loki/Promtail (or equivalent) receiving structured JSON logs

## 2. Security bootstrap

- [ ] `JWT_PRIVATE_KEY_PEM` / public key injected; JWKS reachable
- [ ] `JWT_REQUIRE_ISS_AUD=true` in prod/pilot profiles
- [ ] `CORS_ALLOWED_ORIGINS` set to pilot frontend origins only (or left disabled)
- [ ] Gateway public paths limited to auth/webhooks/health (no accidental open APIs)
- [ ] Stripe/Meta webhook secrets configured if those channels are in scope

## 3. AI / media providers

- [ ] `aisales.ai.llm.provider` ≠ STUB in pilot/prod profile (`ProdLlmGuard`)
- [ ] Embedding provider ≠ STUB in pilot/prod (`ProdEmbeddingGuard`)
- [ ] Media storage `LOCAL` only for ephemeral pilot OR `S3` with private buckets
- [ ] AI daily quotas configured for pilot plan tier

## 4. Tenant + industry

- [ ] Create tenant via tenant-service
- [ ] Create admin user; verify login + refresh + logout
- [ ] Enable industry plugin (`natural-farming` / `real-estate` / `automobile`)
- [ ] Ensure pipeline template via lead pipeline ensure API
- [ ] Seed 1–2 catalog products/offers with industry attribute keys
- [ ] Optional: ingest FAQ knowledge base for RAG

## 5. Smoke tests (happy path)

- [ ] Auth: login → call `GET /api/v1/leads` with Bearer → 200
- [ ] Lead create with industry attributes → event published
- [ ] AI qualify (or execute) → recommendation returned; Lead decides
- [ ] Catalog match/recommend → candidates returned
- [ ] Opportunity + quote with `offerId` → total correct
- [ ] Search returns indexed lead/catalog docs (eventually)
- [ ] Analytics dashboard returns tenant-scoped data
- [ ] Cross-tenant: token A cannot read tenant B resources (404/403)

## 6. Sign-off

| Role | Name | Date | Sign |
|------|------|------|------|
| Delivery Manager | | | |
| DevOps | | | |
| Security | | | |

**Exit:** All sections checked → proceed to [go-live-checklist.md](./go-live-checklist.md)
