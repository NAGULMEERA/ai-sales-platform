-- ============================================================================
-- V022__marketplace.sql
-- Generated from DB.html v7.0: Feature marketplace (§13)
-- Lab monolith only. Split per service for production (UUID refs, no cross-FK).
-- ============================================================================

-- DB.html 13.1 marketplace_listings
CREATE TABLE marketplace_listings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plugin_key TEXT,
    name TEXT,
    description TEXT,
    industry TEXT,
    version TEXT,
    price DECIMAL(19,4),
    enabled BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_marketplace_listings_industry ON marketplace_listings(industry);
CREATE INDEX IF NOT EXISTS idx_marketplace_listings_enabled ON marketplace_listings(enabled);

-- DB.html 13.2 marketplace_installations
CREATE TABLE marketplace_installations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plugin_key TEXT,
    version TEXT,
    status TEXT,
    installed_at TIMESTAMPTZ,
    enabled_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 13.3 marketplace_audit
CREATE TABLE marketplace_audit (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plugin_key TEXT,
    action TEXT,
    performed_by UUID,
    details JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

