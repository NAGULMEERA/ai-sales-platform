-- ============================================================================
-- V012__semantic_cache.sql
-- Semantic Cache: Cache, Hits, Invalidations, Drift
-- ============================================================================

-- ============================================================================
-- semantic_cache
-- ============================================================================
CREATE TABLE semantic_cache (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    query_hash VARCHAR(64) NOT NULL,
    query_text TEXT NOT NULL,
    query_embedding VECTOR(1536),
    response JSONB NOT NULL,
    model_used VARCHAR(100),
    hit_count INTEGER DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_semantic_cache_hash ON semantic_cache(query_hash);
CREATE INDEX idx_semantic_cache_tenant ON semantic_cache(tenant_id);
CREATE INDEX idx_semantic_cache_expires ON semantic_cache(expires_at) WHERE expires_at IS NOT NULL;

ALTER TABLE semantic_cache ADD CONSTRAINT uk_semantic_cache_hash UNIQUE (query_hash);

-- ============================================================================
-- cache_hits
-- ============================================================================
CREATE TABLE cache_hits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cache_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    similarity_score DECIMAL(3,2) NOT NULL,
    cost_saved DECIMAL(19,6),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cache_hits_tenant ON cache_hits(tenant_id, created_at DESC);
CREATE INDEX idx_cache_hits_cache ON cache_hits(cache_id);

-- ============================================================================
-- cache_invalidations
-- ============================================================================
CREATE TABLE cache_invalidations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    cache_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    invalidation_reason VARCHAR(50) NOT NULL,
    invalidated_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_cache_invalidations_tenant ON cache_invalidations(tenant_id);
CREATE INDEX idx_cache_invalidations_cache ON cache_invalidations(cache_id);

-- ============================================================================
-- Foreign Key Constraints
-- ============================================================================
ALTER TABLE semantic_cache ADD CONSTRAINT fk_semantic_cache_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE cache_hits ADD CONSTRAINT fk_cache_hits_cache FOREIGN KEY (cache_id) REFERENCES semantic_cache(id) ON DELETE CASCADE;
ALTER TABLE cache_invalidations ADD CONSTRAINT fk_cache_invalidations_cache FOREIGN KEY (cache_id) REFERENCES semantic_cache(id) ON DELETE CASCADE;
