-- Daily soft-budget reservations (prevents check-then-act races across concurrent AI calls).
CREATE TABLE IF NOT EXISTS tenant_ai_budget_day (
    tenant_id UUID NOT NULL,
    usage_day DATE NOT NULL,
    reserved_total BIGINT NOT NULL DEFAULT 0,
    reserved_execute BIGINT NOT NULL DEFAULT 0,
    reserved_embed BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    PRIMARY KEY (tenant_id, usage_day)
);

-- Scope semantic similarity cache by prompt code/version to avoid cross-prompt collisions.
ALTER TABLE semantic_cache
    ADD COLUMN IF NOT EXISTS prompt_scope VARCHAR(150) NOT NULL DEFAULT '';

ALTER TABLE semantic_cache DROP CONSTRAINT IF EXISTS uk_semantic_cache_tenant_hash;

CREATE UNIQUE INDEX IF NOT EXISTS uk_semantic_cache_tenant_scope_hash_model
    ON semantic_cache (tenant_id, prompt_scope, query_hash, model_used);

CREATE INDEX IF NOT EXISTS idx_semantic_cache_tenant_scope_model
    ON semantic_cache (tenant_id, prompt_scope, model_used);
