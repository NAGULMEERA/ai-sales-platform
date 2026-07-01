# Embedding Strategy

## Core Rule

**Use the same embedding model for both indexing and searching within a given dataset.**

Mixing models in one collection breaks cosine similarity and retrieval quality. Each searchable collection (knowledge base, properties, customer profiles, etc.) binds to exactly one active embedding model at a time. Model changes require re-embedding that collection.

---

## Where Embedding Models Are Used

| # | Use Case | Priority | Owning Service | Storage |
|---|----------|----------|----------------|---------|
| 1 | Knowledge Base (RAG) | ⭐⭐⭐⭐⭐ | AI Service | `document_embeddings` |
| 2 | Property / Catalog Search | ⭐⭐⭐⭐⭐ | Catalog Service | `catalog_embeddings` |
| 3 | Customer Search | ⭐⭐⭐⭐ | Customer Service | `customer_profiles.embedding` |
| 4 | Lead Matching | ⭐⭐⭐⭐ | Lead Service | `leads.embedding` |
| 5 | Conversation Search | ⭐⭐⭐⭐ | Conversation Service | `conversation_summaries.embedding` |
| 6 | AI Memory | ⭐⭐⭐⭐ | AI Service | `conversation_memory`, `agent_memories` |
| 7 | Prompt Search | ⭐⭐⭐ | AI Service | `prompt_templates.embedding` |
| 8 | Semantic Cache | ⭐⭐⭐ | AI Service | `semantic_cache.query_embedding` |
| 9 | Agent Memory | ⭐⭐⭐ | AI Service | `agent_memories.embedding` |
| 10 | Recommendation Engine | ⭐⭐⭐⭐ | Catalog + AI Service | `catalog_embeddings` + RAG |

---

## 1. Knowledge Base (RAG)

Most important use case. Documents ingested from S3, chunked, embedded, stored in pgvector.

**Example documents:** FAQs, property brochures, price lists, bank loan info, legal docs, sales scripts, product catalogs.

```
PDF / DOCX
    │
    ▼
Text Extraction (Media / AI pipeline)
    │
    ▼
Chunking (chunking_strategies)
    │
    ▼
Embedding Model (tenant-configured)
    │
    ▼
pgvector → document_embeddings
```

**Example chunk:**

```
2BHK Apartment
Price: ₹65 Lakhs
Location: Gachibowli
```

↓ embed ↓ store in `document_embeddings` linked to `document_chunks`.

---

## 2. Property Search

Semantic search over catalog items instead of keyword-only queries.

**User query:** *Need family apartment near IT companies under 70 lakhs*

Query is embedded and compared against `catalog_embeddings` for the tenant's active property-search collection.

---

## 3. Customer Search

Customer profile text (budget, location, needs, timeline) is embedded for similarity search.

```
Budget: 50L | Location: Guntur | Needs: Farm Land | Buying in 2 months
    → embedding → customer_profiles.embedding
```

Find customers with similar interests for cross-sell and agent assignment.

---

## 4. Lead Matching

New lead intent embedded and matched against historical leads.

```
"Need villa near airport" → leads.embedding → similar past leads / outcomes
```

---

## 5. Conversation Search

Conversation summaries embedded for salesperson search.

```
Customer asked about: Loan, EMI, Schools nearby
    → conversation_summaries.embedding
```

Query: *Show customers asking about EMI* → semantic search over summaries.

---

## 6. AI Memory

Short- and long-term agent memory stored with embeddings for retrieval during conversations.

```
Customer prefers WhatsApp. Buying after 3 months. Budget 80L.
    → conversation_memory.embedding
```

---

## 7. Prompt Search

Prompt templates embedded so the platform selects the best prompt by semantic similarity to the current task context.

```
"Luxury Property Sales" prompt → prompt_templates.embedding
```

---

## 8. Semantic Cache

Near-duplicate questions reuse cached LLM responses without exact string match.

```
"Need villa Hyderabad" ≈ "Looking for villas in Hyderabad"
    → query_embedding cosine similarity → semantic_cache hit
```

---

## 9. Agent Memory

Per-agent learned context (preferences, call times, objections handled).

```
Agent learned: Customer likes evening calls → agent_memories.embedding
```

---

## 10. Recommendation Engine

User intent embedded; vectors searched across catalog and knowledge base for ranked recommendations.

---

## Provider Strategy (Open Source ↔ Commercial)

The platform supports **switchable embedding backends** per tenant and collection.

| Kind | Default | Hosting | pgvector dims |
|------|---------|---------|---------------|
| **OPEN_SOURCE** | **BAAI/bge-m3** | Self-hosted TEI (`embedding-tei` in Docker) | **1024** |
| COMMERCIAL | OpenAI `text-embedding-3-small` | OpenAI API (enable later) | 1536 |

**Platform default:** `baai` / `BAAI/bge-m3` / **1024** — multilingual (India + global), no API cost, data stays in your infra.

Commercial APIs (OpenAI, Google, Voyage, Cohere) are registered in `embedding_models` with `provider_kind = COMMERCIAL`. Tenants upgrade via `tenant_embedding_config` without code changes.

### Switching providers

```yaml
# application.yml — platform default (open source)
aisales.ai.embedding:
  default-provider-kind: OPEN_SOURCE
  open-source:
    enabled: true
    base-url: http://embedding-tei:80   # or localhost:8099
    model: BAAI/bge-m3
  commercial:
    enabled: false                      # set true when ready
    openai:
      api-key: ${OPENAI_API_KEY}
```

```json
POST /api/v1/embeddings
{
  "collectionKey": "knowledge_base",
  "providerKind": "OPEN_SOURCE",
  "texts": ["2BHK apartment in Gachibowli, ₹65L"]
}
```

To use commercial for a request: `"providerKind": "COMMERCIAL", "modelName": "text-embedding-3-small"` (requires `commercial.enabled=true` and matching vector storage dimension).

---

## Recommended Providers

| Kind | Provider | Model | Dimensions |
|------|----------|-------|------------|
| **OPEN_SOURCE (default)** | BAAI | **bge-m3** | **1024** |
| COMMERCIAL | OpenAI | text-embedding-3-small | 1536 |
| COMMERCIAL | OpenAI | text-embedding-3-large | 3072 |
| COMMERCIAL | Google | text-embedding-004 | 768 |
| COMMERCIAL | Voyage AI | voyage-large-2 | 1536 |
| COMMERCIAL | Cohere | embed-v4 | 1024 |

**Rule:** index and search within a collection must use the **same model and dimension**. Switching provider requires re-embedding that collection.

---

## Database Design

Never store only the vector. Every embedding row includes metadata for governance, re-embedding, and deduplication.

### Standard embedding columns

```sql
embedding           VECTOR(1024)   NOT NULL,   -- default platform dimension (BGE-M3)
embedding_model     VARCHAR(100)   NOT NULL,   -- e.g. BAAI/bge-m3
embedding_provider  VARCHAR(50)    NOT NULL,   -- e.g. baai, openai
embedding_model_version VARCHAR(50),
embedded_at         TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
content_hash        VARCHAR(64)    NOT NULL
```

Commercial models with different dimensions (e.g. 1536, 3072) use separate collections or dedicated columns after tenant upgrade migration.

### Tenant configuration

`tenant_embedding_config` stores per-tenant defaults and per-collection overrides:

| Column | Purpose |
|--------|---------|
| `collection_key` | `knowledge_base`, `property_search`, `customer_search`, … |
| `embedding_model_id` | FK to `embedding_models` |
| `similarity_threshold` | Minimum cosine score for retrieval / cache hit |

### Collection registry

`embedding_collections` enforces one active model per tenant + collection and documents dimension for index validation.

---

## Indexing Rules

1. **HNSW** indexes on pgvector columns (`vector_cosine_ops`) for production retrieval.
2. **Filter by tenant_id** on every query — cross-tenant vector search is prohibited.
3. **Filter by embedding_model** when running queries during model migration (dual-write period).
4. **Composite uniqueness** on `(content_hash, embedding_model, chunk_id)` prevents duplicate embeds.

---

## Re-embedding Workflow

When a tenant upgrades embedding model:

1. Register new model in `embedding_models`.
2. Update `tenant_embedding_config` for affected collections.
3. Background job re-embeds all rows where `embedding_model != new_model`.
4. Swap read traffic to new model after completion.
5. Drop old vectors or archive for rollback.

Skip rows where `content_hash` matches and model already current.

---

## Service Boundaries

| Data | Owner | Never |
|------|-------|-------|
| Knowledge, RAG, semantic cache, prompt embeddings | AI Service | Business services do not own RAG tables |
| Property / catalog embeddings | Catalog Service | Platform Core does not store industry catalog |
| Customer profile embeddings | Customer Service | |
| Lead embeddings | Lead Service | |
| Conversation summary embeddings | Conversation Service | |

Business services call **AI Service** to generate embeddings; they persist vectors in their own schema.

---

## API Contract (AI Service)

```
POST /api/v1/embeddings
  → { tenantId, collectionKey, texts[] }
  ← { embeddings[], model, modelVersion, contentHashes[] }
```

AI Service resolves tenant config, invokes provider abstraction (Spring AI), returns vectors. Callers persist with full metadata.

---

## Observability

Track per tenant and collection:

- Embedding latency and token/cost usage
- Re-embedding job progress
- Cache hit rate (semantic cache)
- Retrieval similarity score distribution
- Model drift (`embedding_drift_score` in observability tables)

---

## Related

- [Database: embedding metadata migration](../../backend/database/migrations-monolith/V033__embedding_metadata.sql)
- Rule 09 — Enterprise AI Engineering Governance
- Rule 05 — pgvector for embeddings only, not transactional data
