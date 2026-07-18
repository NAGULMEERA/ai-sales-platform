# V1 Production Readiness

**Status:** Active after dual-industry vertical validation (Sprints 1–7)  
**Decision:** Architecture freeze holds. Next work is **implementation quality**, then capability gaps (RAG / AI quota).

## Track A — Reliability (do first)

| Priority | Finding | Area | Pass criteria | Status |
|----------|---------|------|---------------|--------|
| P0 | Outbox `DISPATCHING` reclaim | Events | Crash mid-send → event republished within lease | Done (`claimed_at` + lease) |
| P0 | Kafka ack-on-failure | Events | Failures do not silent-ack; Kafka `.DLT` exists | Done (throw + DLT recoverer) |
| P0 | Access-token-only JWT filter | Security | Refresh token rejected on business APIs | Done (`validateAccessToken`) |
| P0 | Real LLM provider for prod | AI | Non-STUB required when `spring.profiles.active=prod` | Done (`ProdLlmGuard`; Stub explicit only) |
| P1 | Feign/LLM outside DB TX | TX | No open connection across remote calls | Done (AI qualify, convert, quote create) |
| P1 | Inbox insert-before-handler | Events | Concurrent redelivery does not double-run handlers | Done (`tryClaim` + ON CONFLICT) |
| P1 | Outbox per-aggregate order | Events | Same aggregateId published sequentially | Done (group by aggregate key) |
| P1 | JWT `organizationId` → TenantContext | Security | Aggregates persist org when claim present | Done (filter sets context) |
| P1 | Trusted-proxy rate-limit key | Gateway | Spoofed XFF cannot bypass auth limits | Done (CIDR-gated XFF) |
| P2 | Idempotency keys | API | Quote / lead create safe under retry | Done (`Idempotency-Key`; plugin enable already upsert) |
| P2 | Quote list EntityGraph | JPA | No N+1 on lineItems | Done (JOIN FETCH) |
| P2 | Named RL policies + `/ai/execute` limit | Gateway | AIChatPolicy enforced at edge | Done (`ai-execute` Redis limiter) |

## Track B — Capability gaps (after Track A)

| # | Item | Pass criteria | Status |
|---|------|---------------|--------|
| 1 | **RAG slice 1** — text index → chunk → embed → pgvector → retrieve → assemble into `POST /ai/execute` | `knowledgeBaseId` on execute returns `retrievedChunks`; doc status READY after `/index` | **Done** (V8 `knowledge_chunk`, `POST .../documents/{id}/index`, gateway retrieve+assemble, local stub embeddings) |
| 2 | **AI Gateway orchestration** | Prompt + knowledge + RAG + LLM router + observability (not proxy-only) | **Done** for core path (execute + RAG + `LlmProviderRouter`; token/cost BC still separate) |
| 3 | **AI cost / quota BC** | Tenant prompt/embedding/vector/LLM budgets | **Done** (LLM + embedding ledger, daily quota, config USD estimates on `token_usage`; invoices via Billing #11) |
| 4 | **Configurable pipelines** | Industry graphs from persisted/plugin metadata | **Done** for RE/Auto (JSON templates in plugins + lead-service classpath; catalog `pipelineTemplate`; Java graphs removed) |
| 5 | **Plugin versioning** | Compatibility + required platform version | **Done** (`min_platform_version` on catalog/descriptor; enable gated by `aisales.platform.version`) |
| 6 | **Real LLM provider impl** | OPENAI/GEMINI/etc. bean (ProdLlmGuard blocks STUB) | **Done** (`OpenAiLlmClient` + `GeminiLlmClient` + `LlmProviderRouter`; switch via `aisales.ai.llm.provider`) |
| 7 | **Pluggable embeddings** | Single provider flag for STUB/TEI/OPENAI | **Done** (`aisales.ai.embedding.provider`; ProdEmbeddingGuard blocks STUB in prod) |
| 8 | **Pluggable chunking** | CHAR / TOKEN switch for RAG index | **Done** (`aisales.ai.rag.chunker`; TOKEN default; sentence-aware token windows) |
| 9 | **Pluggable rerank** | NONE / STUB / TEI after vector retrieve | **Done** (`aisales.ai.rag.reranker`; STUB default; TEI optional cross-encoder) |
| 10 | **Media/S3/PDF ingest** | Upload → extract → chunk → embed | **Done** (media `LOCAL\|S3`; AI `/ingest` + `AUTO\|TEXT\|PDF` extractors; ProdMediaStorageGuard) |
| 11 | **AI usage invoices** | Billing invoice from AI ledger period | **Done** (AI `GET /token-usage/summary`; Billing `POST /invoices/from-ai-usage`) |
| 12 | **Invoice payments** | Pay ISSUED invoices via gateway | **Done** (`POST /invoices/{id}/pay`; plug/flag `STUB\|STRIPE`; ProdPaymentGuard) |
| 13 | **Stripe webhooks** | Complete PENDING payments after client confirm | **Done** (`POST /payments/webhooks/stripe`; signature verify; idempotent PAID) |

**Track B status:** items 1–13 **Done**.

## Explicit non-goals (still hold)

- New industry microservices
- Party / Sales Process rewrite
- Hot plugin classloading
- Expanding public Platform Core APIs unless two industries require the same change

## Review mode

Prefer PR-style findings (N+1, TX boundaries, idempotency, consumer rebalance) over new architectural layers.
