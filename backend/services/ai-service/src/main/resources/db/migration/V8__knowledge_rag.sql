-- Track B: knowledge chunks + pgvector embeddings for RAG retrieval (1024-d BGE-M3).

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE knowledge_chunk (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    knowledge_base_id UUID NOT NULL REFERENCES knowledge_base (id),
    document_id UUID NOT NULL REFERENCES knowledge_document (id),
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    token_estimate INT NOT NULL DEFAULT 0,
    embedding vector(1024),
    embedding_model VARCHAR(100) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_knowledge_chunk_doc_index UNIQUE (document_id, chunk_index)
);

CREATE INDEX idx_knowledge_chunk_kb ON knowledge_chunk (tenant_id, knowledge_base_id);
CREATE INDEX idx_knowledge_chunk_doc ON knowledge_chunk (tenant_id, document_id);
CREATE INDEX idx_knowledge_chunk_hnsw ON knowledge_chunk
    USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;
