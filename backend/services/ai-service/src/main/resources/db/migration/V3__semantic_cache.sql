-- Semantic cache for AI responses (pgvector + JSONB). BGE-M3 default: 1024 dimensions.

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE semantic_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    query_hash VARCHAR(64) NOT NULL,
    query_text TEXT NOT NULL,
    query_embedding vector(1024),
    response JSONB NOT NULL,
    model_used VARCHAR(100) NOT NULL,
    hit_count INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    metadata JSONB DEFAULT '{}'::jsonb,
    CONSTRAINT uk_semantic_cache_tenant_hash UNIQUE (tenant_id, query_hash)
);

CREATE INDEX idx_semantic_cache_tenant ON semantic_cache(tenant_id);
CREATE INDEX idx_semantic_cache_expires ON semantic_cache(expires_at) WHERE expires_at IS NOT NULL;
CREATE INDEX idx_semantic_cache_hnsw ON semantic_cache USING hnsw (query_embedding vector_cosine_ops);
