-- UUID conversation thread + messages (industry-agnostic).
-- Legacy BIGSERIAL "conversations" rows remain unused by the new API.

CREATE TABLE conversation_thread (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    organization_id UUID,
    lead_id UUID,
    customer_id UUID,
    channel VARCHAR(40) NOT NULL DEFAULT 'WEB',
    subject VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    closed_at TIMESTAMPTZ,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_conversation_thread_status CHECK (status IN ('OPEN', 'CLOSED')),
    CONSTRAINT ck_conversation_thread_channel CHECK (
        channel IN ('WEB', 'WHATSAPP', 'EMAIL', 'SMS', 'VOICE', 'OTHER')
    )
);

CREATE INDEX idx_conversation_thread_tenant_status
    ON conversation_thread (tenant_id, status)
    WHERE deleted_at IS NULL;
CREATE INDEX idx_conversation_thread_tenant_lead
    ON conversation_thread (tenant_id, lead_id)
    WHERE deleted_at IS NULL AND lead_id IS NOT NULL;

CREATE TABLE conversation_message (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    conversation_id UUID NOT NULL REFERENCES conversation_thread (id),
    sender_type VARCHAR(30) NOT NULL,
    sender_id UUID,
    body TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    CONSTRAINT ck_conversation_message_sender CHECK (
        sender_type IN ('CUSTOMER', 'AGENT', 'SYSTEM', 'AI')
    )
);

CREATE INDEX idx_conversation_message_conversation_created
    ON conversation_message (conversation_id, created_at);
CREATE INDEX idx_conversation_message_tenant
    ON conversation_message (tenant_id, created_at DESC);
