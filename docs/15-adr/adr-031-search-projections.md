# ADR-031: Enterprise Search Projections

**Status:** Accepted  
**Date:** 2026-07-19  
**Owner:** search-service

## Context

Business services own write models. Clients need cross-entity search (leads, customers, catalog, opportunities, conversations, knowledge) without joining foreign databases.

## Decision

1. **search-service** owns `SearchDocument` projections and query APIs under `/api/v1/search/**`.
2. Projections are updated asynchronously from Kafka integration events (`SearchIndexEventConsumer`).
3. Query modes implemented in search-service include local hybrid/FTS-style search, entity-scoped endpoints, autocomplete, and a semantic path that may call owning services via Feign when configured (`SearchQueryService` / `SearchLocalQueryService`).
4. Business services never write to search tables.

## Consequences

- Search is eventually consistent with outbox/inbox lag.
- Tenant isolation is enforced on every upsert and query (`tenant_id` on `SearchDocument`).
- Gateway rate-limits `/api/v1/search/**` in `GatewayConfig`.

## Related code

- `backend/services/search-service/.../SearchIndexingService.java`
- `backend/services/search-service/.../SearchIndexEventConsumer.java`
- `docs/08-api/api-surface.md`
