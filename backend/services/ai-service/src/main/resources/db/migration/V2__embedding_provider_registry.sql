CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- Embedding model registry (open-source default + commercial alternatives)
-- ============================================================================
CREATE TABLE embedding_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_name VARCHAR(100) NOT NULL,
    model_provider VARCHAR(50) NOT NULL,
    provider_kind VARCHAR(20) NOT NULL DEFAULT 'OPEN_SOURCE',
    dimension INTEGER NOT NULL,
    version VARCHAR(20),
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_embedding_models_provider_kind CHECK (provider_kind IN ('OPEN_SOURCE', 'COMMERCIAL'))
);

CREATE UNIQUE INDEX uk_embedding_models_name_provider ON embedding_models(model_name, model_provider);
CREATE INDEX idx_embedding_models_default ON embedding_models(is_default) WHERE is_default = true;

INSERT INTO embedding_models (id, model_name, model_provider, provider_kind, dimension, version, is_default)
VALUES
    ('00000000-0000-4000-8000-000000000010', 'BAAI/bge-m3', 'baai', 'OPEN_SOURCE', 1024, '1.0', true),
    ('00000000-0000-4000-8000-000000000001', 'text-embedding-3-small', 'openai', 'COMMERCIAL', 1536, '2024-01', false),
    ('00000000-0000-4000-8000-000000000002', 'text-embedding-3-large', 'openai', 'COMMERCIAL', 3072, '2024-01', false),
    ('00000000-0000-4000-8000-000000000003', 'text-embedding-004', 'google', 'COMMERCIAL', 768, '2024-05', false),
    ('00000000-0000-4000-8000-000000000004', 'voyage-large-2', 'voyage', 'COMMERCIAL', 1536, '2024-01', false),
    ('00000000-0000-4000-8000-000000000005', 'embed-v4', 'cohere', 'COMMERCIAL', 1024, '2025-01', false);

-- ============================================================================
-- Per-tenant, per-collection embedding configuration
-- ============================================================================
CREATE TABLE tenant_embedding_config (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    collection_key VARCHAR(100) NOT NULL,
    embedding_model_id UUID NOT NULL REFERENCES embedding_models(id),
    provider_kind VARCHAR(20) NOT NULL DEFAULT 'OPEN_SOURCE',
    similarity_threshold DECIMAL(4,3) NOT NULL DEFAULT 0.850,
    is_active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb,
    CONSTRAINT uk_tenant_embedding_config UNIQUE (tenant_id, collection_key),
    CONSTRAINT ck_tenant_embedding_provider_kind CHECK (provider_kind IN ('OPEN_SOURCE', 'COMMERCIAL'))
);

CREATE INDEX idx_tenant_embedding_config_tenant ON tenant_embedding_config(tenant_id);
