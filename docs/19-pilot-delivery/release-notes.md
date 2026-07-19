# Release Notes — Pilot Build

**Audience:** Pilot customers and Delivery  
**Date:** 2026-07-19  
**Channel:** Design-partner / limited pilot

## Highlights

- Multi-tenant AI Sales Platform Core (Identity, Tenant, Lead, Catalog, Deal, Conversation, Workflow, AI/RAG, Search, Analytics, Billing, Media, Notification, Marketplace, Integration)
- Industry plugins (metadata-only): Real Estate, Automobile, **Natural Farming**
- API Gateway with JWT validation and Redis rate limiting
- Outbox/inbox eventing with DLT recovery patterns
- Production deployment assets: Helm, HPA/PDB/NetworkPolicy, observability stack hooks

## Natural Farming vertical

- Plugin key `natural-farming`  
- Pipeline `NATURAL_FARMING_SALES_V1`  
- Prompts: `LEAD_QUALIFY_NATURAL_FARMING`, `CATALOG_RECOMMEND_NATURAL_FARMING`  
- Docs: [natural-farming-vertical.md](../16-roadmap/natural-farming-vertical.md)

## Reliability & performance (recent)

- Hibernate JDBC batching; Kafka listener concurrency wiring (default 1)  
- Quote catalog offer batch lookup  
- RAG chunk `saveAll` + embedding update batching  
- Search HNSW migration for embeddings  
- Identity prod RBAC cache (Redis)

## Security (recent)

- Media content validation; prompt injection reject  
- Security headers + optional CORS allowlist  
- Shared sensitive data redactor  

## How to upgrade a pilot

1. Review [known-limitations.md](./known-limitations.md)  
2. Apply Flyway via normal rolling deploy ([rolling-deploy-and-flyway.md](../11-devops/rolling-deploy-and-flyway.md))  
3. Run smoke path from [pilot-deployment-checklist.md](./pilot-deployment-checklist.md) §5  
4. Notify CS of any customer-visible API additive changes

## Breaking changes

None intended for pilot APIs (`/api/v1/**`). Additive endpoints only (e.g. catalog offer lookup).
