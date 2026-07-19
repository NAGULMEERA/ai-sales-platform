-- Prefer HNSW for semantic search (works with empty tables; IVFFlat often skipped).
-- Additive / safe: drops optional IVFFlat index if present, then creates HNSW best-effort.

DROP INDEX IF EXISTS idx_search_document_embedding;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_search_document_embedding_hnsw'
    ) THEN
        BEGIN
            CREATE INDEX idx_search_document_embedding_hnsw
                ON search_document USING hnsw (embedding vector_cosine_ops)
                WITH (m = 16, ef_construction = 64);
        EXCEPTION WHEN OTHERS THEN
            -- pgvector / extension version may not support HNSW; leave without vector index.
            NULL;
        END;
    END IF;
END $$;
