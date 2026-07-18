-- Meta (Facebook / Instagram) Lead Ads page → tenant binding and webhook dedup.

CREATE TABLE meta_page_binding (
    id UUID PRIMARY KEY,
    page_id VARCHAR(64) NOT NULL,
    tenant_id UUID NOT NULL,
    organization_id UUID,
    platform VARCHAR(30) NOT NULL DEFAULT 'FACEBOOK',
    source_type VARCHAR(50) NOT NULL DEFAULT 'FACEBOOK_LEAD_ADS',
    campaign_default VARCHAR(255),
    prompt_code VARCHAR(100),
    qualification_variable_keys VARCHAR(500) NOT NULL DEFAULT 'budget,location,timeline',
    voice_qualify_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NOT NULL DEFAULT 'system',
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by VARCHAR(100) NOT NULL DEFAULT 'system',
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_meta_page_binding_page UNIQUE (page_id)
);

CREATE INDEX idx_meta_page_binding_tenant ON meta_page_binding (tenant_id);

CREATE TABLE integration_webhook_event (
    event_id VARCHAR(255) PRIMARY KEY,
    provider VARCHAR(50) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_integration_webhook_event_processed_at ON integration_webhook_event (processed_at);

COMMENT ON TABLE meta_page_binding IS 'Maps Meta page_id to tenant for Lead Ads webhooks';
COMMENT ON TABLE integration_webhook_event IS 'Idempotent webhook delivery claims (Meta leadgen_id, etc.)';
