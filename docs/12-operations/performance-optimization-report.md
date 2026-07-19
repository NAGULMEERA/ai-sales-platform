# Performance Optimization Report — AI Sales Platform

**Role:** Performance Engineer  
**Date:** 2026-07-19  
**Scope:** Safe optimizations only — no functional behavior changes, no destructive schema changes, APIs remain backward compatible.

## Executive summary

Baseline already included strong defaults (Hikari pools, `open-in-view: false`, Kafka producer batching, AI embedding batch HTTP, knowledge HNSW, quote JOIN FETCH, Docker `MaxRAMPercentage=75`). This pass closed the highest-impact remaining gaps: Hibernate JDBC batching, Kafka listener concurrency wiring, RAG chunk persist batching, catalog offer Feign N+1 on quote create, search embedding HNSW, and identity RBAC cache enablement in prod.

| Area | Before | After | Risk |
|------|--------|-------|------|
| JPA inserts/updates | Single-row JDBC | `batch_size=50`, ordered inserts/updates | Low |
| Kafka consumers | Factory concurrency unset (1) | Property-driven concurrency (default 1) | Low |
| RAG chunk persist | Per-chunk `save` + UPDATE | `saveAll` + batched embedding updates | Low |
| Quote create Feign | 1 call per line offer | `POST /offers/lookup` batch | Low (additive API) |
| Search vectors | IVFFlat often skipped on empty table | HNSW migration V4 | Low (best-effort) |
| Identity RBAC cache | Off by default | Prod `AISALES_CACHE_ENABLED=true` | Low (needs Redis) |

## Inventory (reused, not duplicated)

- Hikari / lifecycle / OTLP / Micrometer: `platform/application-observability.yml`
- Redis cache abstraction: `common-cache` + `PlatformCacheService` / `RbacService`
- Kafka listener factory: `KafkaEventsAutoConfiguration`
- AI RAG pipeline: `DocumentEmbeddingPipeline` + `KnowledgeChunkVectorRepository`
- Catalog contracts / Feign: `CatalogServiceClient`, `CatalogOfferDto`
- Search Flyway: `search-service` V1–V3 (extended with V4)

## Changes delivered

### Database / JPA

- Enabled Hibernate JDBC batching platform-wide via observability YAML.
- Search: Flyway `V4__search_embedding_hnsw.sql` replaces optional IVFFlat with HNSW when pgvector supports it.

### Caching / Redis

- Identity `application-prod.yml` enables `aisales.cache` with env override (`AISALES_CACHE_ENABLED`, `AISALES_CACHE_TTL`). Local/dev remain opt-in via cache YAML defaults.

### Kafka

- Tuned producer linger/batch and consumer `max-poll-records` (env-overridable).
- `integrationKafkaListenerContainerFactory` now applies `spring.kafka.listener.concurrency` (default **1** preserves partition ordering).

### AI / RAG

- Chunk rows persisted with `saveAll`; embeddings applied via `updateEmbeddings` (flush once after loop).
- Embedding HTTP batching and knowledge HNSW indexes were already present — left unchanged.

### API latency (quotes)

- Additive `POST /api/v1/offers/lookup` + Feign `lookupOffers`.
- `QuoteService.create` resolves distinct offer ids in one gateway call. Existing `GET /offers/{id}` unchanged.

### Observability

- Existing Micrometer Prometheus + OTLP tracing retained.
- Recommended SLOs remain in `docs/12-operations/alerting-and-slos.md`.

## Intentionally not changed

- No CQRS/event-sourcing redesign, no engine swaps, no package renames.
- Default Kafka concurrency stays **1** (raise per service only when consumers are idempotent and lag justifies it).
- No aggressive GC flags beyond existing container `MaxRAMPercentage`.
- Workflow / Temporal not introduced; workflow service orchestration left as-is.

## Validation

- Unit: deal-service quote / industry quote flow tests updated for batch lookup.
- Recommend: run catalog-service + deal-service + ai-service unit suites; apply search Flyway V4 in a non-prod DB and confirm `idx_search_document_embedding_hnsw`.

## Follow-ups (out of scope for this pass)

1. True multi-row embedding UPDATE (UNNEST) if native batch UPDATE becomes a hotspot.
2. Per-service Hikari / Kafka concurrency profiles under load (k6 + Grafana).
3. Prompt / semantic-cache hit-rate dashboards tied to AI token cost.
4. Connection-pool and GC baselines under production-like concurrency.
