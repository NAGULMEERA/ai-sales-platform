-- ============================================================================
-- V033__embedding_metadata.sql
-- Embedding governance: metadata columns, tenant config, missing use-case vectors
-- ============================================================================
-- Rule: same embedding model for index + search within each collection.
-- See docs/06-ai-platform/embedding-strategy.md
-- ============================================================================

-- ============================================================================
-- Seed supported embedding models
-- ============================================================================
INSERT INTO embedding_models (id, model_name, model_provider, dimension, version, is_default)
VALUES
    ('00000000-0000-4000-8000-000000000001', 'text-embedding-3-small', 'openai', 1536, '2024-01', true),
    ('00000000-0000-4000-8000-000000000002', 'text-embedding-3-large', 'openai', 3072, '2024-01', false),
    ('00000000-0000-4000-8000-000000000003', 'text-embedding-004', 'google', 768, '2024-05', false),
    ('00000000-0000-4000-8000-000000000004', 'voyage-large-2', 'voyage', 1536, '2024-01', false),
    ('00000000-0000-4000-8000-000000000005', 'embed-v4', 'cohere', 1024, '2025-01', false)
ON CONFLICT (id) DO NOTHING;

-- ============================================================================
-- tenant_embedding_config — per-tenant, per-collection model binding
-- ============================================================================
CREATE TABLE IF NOT EXISTS tenant_embedding_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    collection_key VARCHAR(100) NOT NULL,
    embedding_model_id UUID NOT NULL,
    similarity_threshold DECIMAL(4,3) NOT NULL DEFAULT 0.850,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb,
    CONSTRAINT uk_tenant_embedding_config UNIQUE (tenant_id, collection_key)
);

CREATE INDEX IF NOT EXISTS idx_tenant_embedding_config_tenant ON tenant_embedding_config(tenant_id);

ALTER TABLE tenant_embedding_config
    ADD CONSTRAINT fk_tenant_embedding_config_tenant
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;

ALTER TABLE tenant_embedding_config
    ADD CONSTRAINT fk_tenant_embedding_config_model
    FOREIGN KEY (embedding_model_id) REFERENCES embedding_models(id);

-- ============================================================================
-- Standard metadata on existing embedding tables
-- ============================================================================

-- document_embeddings (RAG / knowledge base)
ALTER TABLE document_embeddings
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedding_model_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);

UPDATE document_embeddings SET
    embedding_model = 'text-embedding-3-small',
    embedding_provider = 'openai',
    embedded_at = COALESCE(embedded_at, created_at)
WHERE embedding_model IS NULL;

-- catalog_embeddings (property / catalog search)
ALTER TABLE catalog_embeddings
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedding_model_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);

UPDATE catalog_embeddings SET
    embedding_model = 'text-embedding-3-small',
    embedding_provider = 'openai',
    embedded_at = COALESCE(embedded_at, created_at)
WHERE embedding_model IS NULL;

-- semantic_cache
ALTER TABLE semantic_cache
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedding_model_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMPTZ DEFAULT NOW();

UPDATE semantic_cache SET
    embedding_model = COALESCE(model_used, 'text-embedding-3-small'),
    embedding_provider = 'openai',
    embedded_at = COALESCE(embedded_at, created_at)
WHERE embedding_model IS NULL;

-- conversation_memory (AI memory)
ALTER TABLE conversation_memory
    ADD COLUMN IF NOT EXISTS tenant_id UUID,
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedding_model_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);

UPDATE conversation_memory SET
    embedding_model = 'text-embedding-3-small',
    embedding_provider = 'openai',
    embedded_at = COALESCE(embedded_at, created_at)
WHERE embedding_model IS NULL;

-- agent_memories (agent memory)
ALTER TABLE agent_memories
    ADD COLUMN IF NOT EXISTS tenant_id UUID,
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedding_model_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMPTZ DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);

UPDATE agent_memories SET
    embedding_model = 'text-embedding-3-small',
    embedding_provider = 'openai',
    embedded_at = COALESCE(embedded_at, created_at)
WHERE embedding_model IS NULL;

-- ============================================================================
-- New embedding columns for use cases not yet vectorized
-- ============================================================================

-- customer_profiles (customer search)
ALTER TABLE customer_profiles
    ADD COLUMN IF NOT EXISTS embedding VECTOR(1536),
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedding_model_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_customer_profiles_embedding_hnsw
    ON customer_profiles USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

-- leads (lead matching)
ALTER TABLE leads
    ADD COLUMN IF NOT EXISTS embedding VECTOR(1536),
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedding_model_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_leads_embedding_hnsw
    ON leads USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

-- conversation_summaries (conversation search)
ALTER TABLE conversation_summaries
    ADD COLUMN IF NOT EXISTS tenant_id UUID,
    ADD COLUMN IF NOT EXISTS embedding VECTOR(1536),
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedding_model_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_conversation_summaries_embedding_hnsw
    ON conversation_summaries USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

-- prompt_templates (prompt search)
ALTER TABLE prompt_templates
    ADD COLUMN IF NOT EXISTS embedding VECTOR(1536),
    ADD COLUMN IF NOT EXISTS embedding_model VARCHAR(100),
    ADD COLUMN IF NOT EXISTS embedding_provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedding_model_version VARCHAR(50),
    ADD COLUMN IF NOT EXISTS embedded_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS content_hash VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_prompt_templates_embedding_hnsw
    ON prompt_templates USING hnsw (embedding vector_cosine_ops)
    WHERE embedding IS NOT NULL;

-- ============================================================================
-- Deduplication indexes (content_hash + model per source row)
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_document_embeddings_content_hash
    ON document_embeddings(content_hash, embedding_model)
    WHERE content_hash IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_catalog_embeddings_content_hash
    ON catalog_embeddings(content_hash, embedding_model)
    WHERE content_hash IS NOT NULL;

-- ============================================================================
-- Collection keys reference (documentation constraint)
-- knowledge_base | property_search | customer_search | lead_matching
-- conversation_search | ai_memory | prompt_search | semantic_cache
-- agent_memory | recommendation
-- ============================================================================
