-- ============================================================================
-- V035__bge_m3_default_embeddings.sql
-- Default open-source embeddings: BAAI/bge-m3 (1024 dims)
-- Commercial providers remain available via tenant_embedding_config
-- See docs/06-ai-platform/embedding-strategy.md
-- ============================================================================

ALTER TABLE embedding_models
    ADD COLUMN IF NOT EXISTS provider_kind VARCHAR(20) NOT NULL DEFAULT 'COMMERCIAL';

ALTER TABLE embedding_models
    ADD CONSTRAINT ck_embedding_models_provider_kind
    CHECK (provider_kind IN ('OPEN_SOURCE', 'COMMERCIAL'));

-- Reset defaults: open-source BGE-M3 is platform default
UPDATE embedding_models SET is_default = false;

INSERT INTO embedding_models (id, model_name, model_provider, provider_kind, dimension, version, is_default)
VALUES
    ('00000000-0000-4000-8000-000000000010', 'BAAI/bge-m3', 'baai', 'OPEN_SOURCE', 1024, '1.0', true)
ON CONFLICT (id) DO UPDATE SET
    model_name = EXCLUDED.model_name,
    model_provider = EXCLUDED.model_provider,
    provider_kind = EXCLUDED.provider_kind,
    dimension = EXCLUDED.dimension,
    version = EXCLUDED.version,
    is_default = EXCLUDED.is_default;

UPDATE embedding_models SET provider_kind = 'OPEN_SOURCE' WHERE model_provider = 'baai';
UPDATE embedding_models SET provider_kind = 'COMMERCIAL' WHERE model_provider IN ('openai', 'google', 'voyage', 'cohere');

-- Drop HNSW / vector indexes before dimension migration
DROP INDEX IF EXISTS idx_catalog_embeddings_hnsw;
DROP INDEX IF EXISTS idx_document_embeddings_hnsw;
DROP INDEX IF EXISTS idx_semantic_cache_hnsw;
DROP INDEX IF EXISTS idx_conversation_memory_hnsw;
DROP INDEX IF EXISTS idx_conversation_memory_vector;
DROP INDEX IF EXISTS idx_agent_memory_hnsw;
DROP INDEX IF EXISTS idx_agent_memories_hnsw;
DROP INDEX IF EXISTS idx_customer_profiles_embedding_hnsw;
DROP INDEX IF EXISTS idx_leads_embedding_hnsw;
DROP INDEX IF EXISTS idx_conversation_summaries_embedding_hnsw;
DROP INDEX IF EXISTS idx_prompt_templates_embedding_hnsw;

-- Clear incompatible 1536-dim vectors (lab); production re-embeds via AI Service job
UPDATE document_embeddings SET embedding = NULL WHERE embedding IS NOT NULL;
UPDATE catalog_embeddings SET embedding = NULL WHERE embedding IS NOT NULL;
UPDATE semantic_cache SET query_embedding = NULL WHERE query_embedding IS NOT NULL;
UPDATE conversation_memory SET embedding = NULL WHERE embedding IS NOT NULL;
UPDATE agent_memories SET embedding = NULL WHERE embedding IS NOT NULL;
UPDATE customer_profiles SET embedding = NULL WHERE embedding IS NOT NULL;
UPDATE leads SET embedding = NULL WHERE embedding IS NOT NULL;
UPDATE conversation_summaries SET embedding = NULL WHERE embedding IS NOT NULL;
UPDATE prompt_templates SET embedding = NULL WHERE embedding IS NOT NULL;

-- Migrate column types to 1024 (BGE-M3)
ALTER TABLE document_embeddings ALTER COLUMN embedding TYPE vector(1024);
ALTER TABLE catalog_embeddings ALTER COLUMN embedding TYPE vector(1024);
ALTER TABLE semantic_cache ALTER COLUMN query_embedding TYPE vector(1024);
ALTER TABLE conversation_memory ALTER COLUMN embedding TYPE vector(1024);
ALTER TABLE agent_memories ALTER COLUMN embedding TYPE vector(1024);

DO $$ BEGIN
    ALTER TABLE customer_profiles ALTER COLUMN embedding TYPE vector(1024);
EXCEPTION WHEN undefined_column THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE leads ALTER COLUMN embedding TYPE vector(1024);
EXCEPTION WHEN undefined_column THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE conversation_summaries ALTER COLUMN embedding TYPE vector(1024);
EXCEPTION WHEN undefined_column THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE prompt_templates ALTER COLUMN embedding TYPE vector(1024);
EXCEPTION WHEN undefined_column THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE agent_memory ALTER COLUMN embedding TYPE vector(1024);
EXCEPTION WHEN undefined_table THEN NULL;
END $$;

DO $$ BEGIN
    ALTER TABLE ai_drift_detection ALTER COLUMN baseline_embedding TYPE vector(1024);
    ALTER TABLE ai_drift_detection ALTER COLUMN current_embedding TYPE vector(1024);
EXCEPTION WHEN undefined_table THEN NULL;
END $$;

-- Default metadata for open-source model
UPDATE document_embeddings SET embedding_model = 'BAAI/bge-m3', embedding_provider = 'baai' WHERE embedding_model IS NULL OR embedding_model = 'text-embedding-3-small';
UPDATE catalog_embeddings SET embedding_model = 'BAAI/bge-m3', embedding_provider = 'baai' WHERE embedding_model IS NULL OR embedding_model = 'text-embedding-3-small';
UPDATE semantic_cache SET embedding_model = 'BAAI/bge-m3', embedding_provider = 'baai' WHERE embedding_model IS NULL OR embedding_model LIKE 'text-embedding%';
UPDATE conversation_memory SET embedding_model = 'BAAI/bge-m3', embedding_provider = 'baai' WHERE embedding_model IS NULL OR embedding_model = 'text-embedding-3-small';
UPDATE agent_memories SET embedding_model = 'BAAI/bge-m3', embedding_provider = 'baai' WHERE embedding_model IS NULL OR embedding_model = 'text-embedding-3-small';

-- Recreate HNSW indexes (1024 dims)
CREATE INDEX IF NOT EXISTS idx_document_embeddings_hnsw ON document_embeddings USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_catalog_embeddings_hnsw ON catalog_embeddings USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_semantic_cache_hnsw ON semantic_cache USING hnsw (query_embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_conversation_memory_hnsw ON conversation_memory USING hnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_agent_memories_hnsw ON agent_memories USING hnsw (embedding vector_cosine_ops);

CREATE INDEX IF NOT EXISTS idx_customer_profiles_embedding_hnsw
    ON customer_profiles USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_leads_embedding_hnsw
    ON leads USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_conversation_summaries_embedding_hnsw
    ON conversation_summaries USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_prompt_templates_embedding_hnsw
    ON prompt_templates USING hnsw (embedding vector_cosine_ops) WHERE embedding IS NOT NULL;

-- Point default tenant config to BGE-M3 for all collections (idempotent seed for lab)
INSERT INTO tenant_embedding_config (tenant_id, collection_key, embedding_model_id, similarity_threshold)
SELECT t.id, c.collection_key, '00000000-0000-4000-8000-000000000010'::uuid, 0.850
FROM tenants t
CROSS JOIN (VALUES
    ('knowledge_base'),
    ('property_search'),
    ('customer_search'),
    ('lead_matching'),
    ('conversation_search'),
    ('ai_memory'),
    ('prompt_search'),
    ('semantic_cache'),
    ('agent_memory'),
    ('recommendation')
) AS c(collection_key)
ON CONFLICT (tenant_id, collection_key) DO UPDATE SET
    embedding_model_id = EXCLUDED.embedding_model_id;
