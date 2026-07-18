-- Outbound voice call tracking for provider status callbacks (Twilio CallSid → lead).
CREATE TABLE voice_call (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    lead_id UUID NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_call_id VARCHAR(255) NOT NULL,
    to_phone VARCHAR(50),
    status VARCHAR(40) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_voice_call_provider_call UNIQUE (provider, provider_call_id)
);

CREATE INDEX idx_voice_call_tenant_lead ON voice_call (tenant_id, lead_id);

COMMENT ON TABLE voice_call IS 'Outbound voice calls placed by integration-service voice providers';
