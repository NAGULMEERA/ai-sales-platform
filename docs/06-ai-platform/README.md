# AI Platform

AI architecture for the multi-tenant AI Sales Employee Platform.

## Documents

| Document | Description |
|----------|-------------|
| [Embedding Strategy](embedding-strategy.md) | Where embeddings are used, provider selection, pgvector design, tenant configuration |

## Principles

- AI recommends; business services decide.
- Same embedding model for index and search within each collection.
- Embeddings stored in pgvector with model metadata for re-embedding and deduplication.
- Default **open-source** embeddings: **BAAI/bge-m3** (1024 dims) via self-hosted TEI
- **Switchable** open-source ↔ commercial providers per tenant/collection
- Commercial APIs (OpenAI, etc.) enabled when `aisales.ai.embedding.commercial.enabled=true`
