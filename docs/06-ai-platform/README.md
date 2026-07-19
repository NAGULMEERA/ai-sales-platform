# AI Platform

AI architecture for the multi-tenant AI Sales Employee Platform. Implementation lives in **ai-service**.

## Documents

| Document | Description |
|----------|-------------|
| [ai-architecture.md](ai-architecture.md) | Gateway, prompts, execute path, quota, providers |
| [rag-architecture.md](rag-architecture.md) | Ingest, chunk, embed, hybrid retrieve, context assembly |
| [Embedding Strategy](embedding-strategy.md) | Embedding providers, pgvector, tenant configuration |

## Principles (enforced in code)

- AI recommends; business services decide.
- Business services never call LLM vendor SDKs directly.
- Same embedding model for index and search within each collection.
- Embeddings stored in pgvector with model metadata.
- Prompt variables sanitized; RAG context treated as untrusted data.
