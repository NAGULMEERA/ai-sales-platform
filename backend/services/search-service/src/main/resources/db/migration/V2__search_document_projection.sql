-- Enterprise search projection: FTS + optional pgvector + metadata facets.

CREATE EXTENSION IF NOT EXISTS vector;

ALTER TABLE search_document
    ADD COLUMN IF NOT EXISTS entity_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS entity_id UUID,
    ADD COLUMN IF NOT EXISTS title VARCHAR(500),
    ADD COLUMN IF NOT EXISTS body TEXT,
    ADD COLUMN IF NOT EXISTS keywords VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS status VARCHAR(60),
    ADD COLUMN IF NOT EXISTS source VARCHAR(100),
    ADD COLUMN IF NOT EXISTS popularity BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS business_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS source_updated_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    ADD COLUMN IF NOT EXISTS search_vector TSVECTOR,
    ADD COLUMN IF NOT EXISTS embedding VECTOR(1024);

UPDATE search_document
SET entity_type = 'LEAD',
    entity_id = id,
    title = COALESCE(title, 'Untitled'),
    body = COALESCE(body, '')
WHERE entity_type IS NULL;

ALTER TABLE search_document
    ALTER COLUMN entity_type SET NOT NULL,
    ALTER COLUMN entity_id SET NOT NULL,
    ALTER COLUMN title SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_search_document_tenant_entity
    ON search_document (tenant_id, entity_type, entity_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_search_document_tenant_type
    ON search_document (tenant_id, entity_type)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_search_document_fts
    ON search_document USING GIN (search_vector);

CREATE INDEX IF NOT EXISTS idx_search_document_title_trgm
    ON search_document (tenant_id, lower(title) text_pattern_ops);

CREATE INDEX IF NOT EXISTS idx_search_document_status
    ON search_document (tenant_id, entity_type, status)
    WHERE deleted_at IS NULL;

-- Optional IVFFlat index when embeddings exist (safe if empty).
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes WHERE indexname = 'idx_search_document_embedding'
    ) THEN
        BEGIN
            CREATE INDEX idx_search_document_embedding
                ON search_document USING ivfflat (embedding vector_cosine_ops)
                WITH (lists = 100);
        EXCEPTION WHEN OTHERS THEN
            -- IVFFlat requires data; skip until embeddings are populated.
            NULL;
        END;
    END IF;
END $$;
