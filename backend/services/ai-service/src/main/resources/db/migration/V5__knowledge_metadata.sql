-- Knowledge metadata only. Binary content lives in media-service / S3.
-- Chunking, embeddings, and RAG retrieval are out of Phase 4 MVP scope.

CREATE TABLE knowledge_base (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    organization_id UUID,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_knowledge_base_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'ARCHIVED')),
    CONSTRAINT uq_knowledge_base_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_knowledge_base_tenant_status ON knowledge_base (tenant_id, status)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_knowledge_base_updated_at
    BEFORE UPDATE ON knowledge_base
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TABLE knowledge_document (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    knowledge_base_id UUID NOT NULL REFERENCES knowledge_base (id),
    name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100),
    size_bytes BIGINT,
    media_id UUID,
    object_key VARCHAR(1000),
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_knowledge_document_status CHECK (
        status IN ('PENDING', 'READY', 'FAILED', 'ARCHIVED')
    )
);

CREATE INDEX idx_knowledge_document_kb ON knowledge_document (tenant_id, knowledge_base_id)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_knowledge_document_updated_at
    BEFORE UPDATE ON knowledge_document
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
