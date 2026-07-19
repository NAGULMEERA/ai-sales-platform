# RAG Architecture

All RAG components live in **ai-service** under `com.aisales.ai.application.rag` and related services.

## Data model

| Entity | Role |
|--------|------|
| `KnowledgeBase` | Tenant-scoped KB |
| `KnowledgeDocument` | Document metadata (binary via media-service / S3) |
| `KnowledgeChunk` | Chunk text + embedding (pgvector) |

## Ingest / index pipeline (implemented)

1. Register document (`KnowledgeController` → knowledge-documents APIs)
2. Ingest content (`KnowledgeIngestService`) using `DocumentExtractor` registry (`TextDocumentExtractor`, `PdfDocumentExtractor`)
3. Chunk (`TokenWindowChunker` / `CharWindowChunker` / `TextChunker`)
4. Embed via embedding provider registry
5. Persist chunks (`KnowledgeIndexingService` / `DocumentEmbeddingPipeline`)
6. Publish `KnowledgeDocumentRegistered` (consumed by search indexer)

## Retrieval

| Component | Role |
|-----------|------|
| `VectorRetriever` | Dense similarity over pgvector |
| `KeywordRetriever` | Full-text / keyword hits |
| `HybridRetriever` | Reciprocal Rank Fusion (RRF) of vector + keyword |
| `RetrieverRegistry` | Selects retriever by name (e.g. `HYBRID`) |
| `Reranker` / `RerankerRegistry` | Optional rerank (`StubReranker`, `TeiReranker`, `NoneReranker`) |
| `KnowledgeRetrievalService` | Orchestrates retrieve for gateway |
| `KnowledgeContextAssembler` | Builds prompt section with `<<<KNOWLEDGE_DATA>>>` untrusted delimiters |

## Tenant isolation

Retrieval and indexing always filter by tenant ownership of the knowledge base/document. Cross-tenant vector search is not supported.

## Events

- `KnowledgeBaseCreated`
- `KnowledgeDocumentRegistered`
- `KnowledgeRetrieved` (also recorded by analytics as `rag.request`)

## Related

- [ai-architecture.md](ai-architecture.md)
- [embedding-strategy.md](embedding-strategy.md)
- [ADR-024](../15-adr/adr-024-ai-gateway.md)
