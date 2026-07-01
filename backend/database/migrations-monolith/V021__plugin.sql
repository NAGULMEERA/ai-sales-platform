-- ============================================================================
-- V021__plugin.sql
-- Generated from DB.html v7.0: Plugin lifecycle (§19)
-- Lab monolith only. Split per service for production (UUID refs, no cross-FK).
-- ============================================================================

-- DB.html 19.1 plugin_registry
CREATE TABLE plugin_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    industry_name TEXT,
    module_class TEXT,
    enabled BOOLEAN DEFAULT false,
    config JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_plugin_registry_industry ON plugin_registry(industry_name);
CREATE INDEX IF NOT EXISTS idx_plugin_registry_enabled ON plugin_registry(enabled);

-- DB.html 19.2 plugin_versions
CREATE TABLE plugin_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_registry_id UUID,
    version TEXT,
    changelog TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 19.3 plugin_installations
CREATE TABLE plugin_installations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plugin_registry_id UUID,
    version TEXT,
    status TEXT,
    installed_at TIMESTAMPTZ,
    enabled_at TIMESTAMPTZ
);


-- DB.html 19.4 plugin_audit
CREATE TABLE plugin_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_registry_id UUID,
    action TEXT,
    performed_by UUID,
    details JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

