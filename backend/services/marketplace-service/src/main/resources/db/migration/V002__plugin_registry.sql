-- Thin plugin catalog + per-tenant installations (marketplace-service owns registry).
-- No licensing, billing, or dynamic classloading in Phase 5.

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE plugin_catalog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_key VARCHAR(100) NOT NULL,
    plugin_type VARCHAR(30) NOT NULL,
    version VARCHAR(50) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    capabilities JSONB NOT NULL DEFAULT '[]'::jsonb,
    industry_code VARCHAR(50),
    config_schema_json TEXT NOT NULL DEFAULT '{}',
    default_config JSONB NOT NULL DEFAULT '{}'::jsonb,
    metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
    available BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_plugin_catalog_type CHECK (plugin_type IN ('CAPABILITY', 'INDUSTRY')),
    CONSTRAINT uq_plugin_catalog_key UNIQUE (plugin_key)
);

CREATE INDEX idx_plugin_catalog_type ON plugin_catalog (plugin_type) WHERE available = TRUE;

CREATE TRIGGER trg_plugin_catalog_updated_at
    BEFORE UPDATE ON plugin_catalog
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TABLE plugin_installation (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plugin_key VARCHAR(100) NOT NULL REFERENCES plugin_catalog (plugin_key),
    version VARCHAR(50) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DISABLED',
    config JSONB NOT NULL DEFAULT '{}'::jsonb,
    enabled_at TIMESTAMPTZ,
    disabled_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version_lock BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_plugin_installation_status CHECK (status IN ('ENABLED', 'DISABLED')),
    CONSTRAINT uq_plugin_installation_tenant_key UNIQUE (tenant_id, plugin_key)
);

CREATE INDEX idx_plugin_installation_tenant_status ON plugin_installation (tenant_id, status);

CREATE TRIGGER trg_plugin_installation_updated_at
    BEFORE UPDATE ON plugin_installation
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();
