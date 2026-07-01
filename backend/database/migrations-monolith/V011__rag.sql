-- ============================================================================
-- V011__rag.sql
-- RAG: Knowledge Bases, Documents, Chunks, Embeddings
-- ============================================================================

-- ============================================================================
-- knowledge_bases
-- ============================================================================
CREATE TABLE knowledge_bases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    kb_type VARCHAR(50) NOT NULL,
    config JSONB DEFAULT '{}'::jsonb,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_knowledge_bases_tenant ON knowledge_bases(tenant_id);
CREATE INDEX idx_knowledge_bases_type ON knowledge_bases(kb_type);

-- ============================================================================
-- knowledge_base_versions
-- ============================================================================
CREATE TABLE knowledge_base_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_base_id UUID NOT NULL,
    version INTEGER NOT NULL,
    config JSONB DEFAULT '{}'::jsonb,
    changelog TEXT,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_kb_versions_base ON knowledge_base_versions(knowledge_base_id, version DESC);

-- ============================================================================
-- knowledge_documents
-- ============================================================================
CREATE TABLE knowledge_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_base_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    file_type VARCHAR(50) NOT NULL,
    file_size BIGINT,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    content TEXT,
    metadata JSONB DEFAULT '{}'::jsonb,
    processed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_knowledge_documents_base ON knowledge_documents(knowledge_base_id);
CREATE INDEX idx_knowledge_documents_status ON knowledge_documents(status);

-- ============================================================================
-- document_chunks
-- ============================================================================
CREATE TABLE document_chunks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id UUID NOT NULL,
    chunk_index INTEGER NOT NULL,
    chunk_text TEXT NOT NULL,
    chunk_metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_document_chunks_document ON document_chunks(document_id);

-- ============================================================================
-- document_embeddings
-- ============================================================================
CREATE TABLE document_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    chunk_id UUID NOT NULL,
    embedding VECTOR(1536),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- ============================================================================
-- chunking_strategies
-- ============================================================================
CREATE TABLE chunking_strategies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    strategy_type VARCHAR(50) NOT NULL,
    chunk_size INTEGER NOT NULL,
    overlap INTEGER NOT NULL,
    config JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE chunking_strategies ADD CONSTRAINT uk_chunking_strategies_type UNIQUE (strategy_type);

-- ============================================================================
-- embedding_models
-- ============================================================================
CREATE TABLE embedding_models (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    model_name VARCHAR(100) NOT NULL,
    model_provider VARCHAR(50) NOT NULL,
    dimension INTEGER NOT NULL,
    version VARCHAR(20),
    is_default BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_embedding_models_default ON embedding_models(is_default) WHERE is_default = true;

-- ============================================================================
-- knowledge_sources
-- ============================================================================
CREATE TABLE knowledge_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    knowledge_base_id UUID NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_config JSONB DEFAULT '{}'::jsonb,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_knowledge_sources_base ON knowledge_sources(knowledge_base_id);

-- ============================================================================
-- rag_pipelines
-- ============================================================================
CREATE TABLE rag_pipelines (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    pipeline_name VARCHAR(255) NOT NULL,
    pipeline_type VARCHAR(50) NOT NULL,
    chunking_strategy_id UUID,
    embedding_model_id UUID,
    retrieval_config JSONB DEFAULT '{}'::jsonb,
    generation_config JSONB DEFAULT '{}'::jsonb,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rag_pipelines_tenant ON rag_pipelines(tenant_id);
CREATE INDEX idx_rag_pipelines_type ON rag_pipelines(pipeline_type);

-- ============================================================================
-- retrieval_logs
-- ============================================================================
CREATE TABLE retrieval_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    rag_pipeline_id UUID NOT NULL,
    query TEXT NOT NULL,
    retrieved_chunks JSONB DEFAULT '{}'::jsonb,
    top_k INTEGER,
    retrieval_time_ms INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_retrieval_logs_pipeline ON retrieval_logs(rag_pipeline_id, created_at DESC);

-- ============================================================================
-- Foreign Key Constraints
-- ============================================================================
ALTER TABLE knowledge_bases ADD CONSTRAINT fk_knowledge_bases_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE knowledge_base_versions ADD CONSTRAINT fk_kb_versions_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases(id) ON DELETE CASCADE;
ALTER TABLE knowledge_documents ADD CONSTRAINT fk_knowledge_documents_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases(id) ON DELETE CASCADE;
ALTER TABLE document_chunks ADD CONSTRAINT fk_document_chunks_document FOREIGN KEY (document_id) REFERENCES knowledge_documents(id) ON DELETE CASCADE;
ALTER TABLE document_embeddings ADD CONSTRAINT fk_document_embeddings_chunk FOREIGN KEY (chunk_id) REFERENCES document_chunks(id) ON DELETE CASCADE;
ALTER TABLE knowledge_sources ADD CONSTRAINT fk_knowledge_sources_base FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_bases(id) ON DELETE CASCADE;
ALTER TABLE rag_pipelines ADD CONSTRAINT fk_rag_pipelines_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE rag_pipelines ADD CONSTRAINT fk_rag_pipelines_chunking FOREIGN KEY (chunking_strategy_id) REFERENCES chunking_strategies(id);
ALTER TABLE rag_pipelines ADD CONSTRAINT fk_rag_pipelines_embedding FOREIGN KEY (embedding_model_id) REFERENCES embedding_models(id);
ALTER TABLE retrieval_logs ADD CONSTRAINT fk_retrieval_logs_pipeline FOREIGN KEY (rag_pipeline_id) REFERENCES rag_pipelines(id) ON DELETE CASCADE;
