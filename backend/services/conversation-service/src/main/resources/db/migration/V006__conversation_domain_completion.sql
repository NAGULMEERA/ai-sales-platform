-- Conversation domain completion: message delivery, participants, attachments, AI context fields.

ALTER TABLE conversation_thread
    ADD COLUMN IF NOT EXISTS opportunity_id UUID,
    ADD COLUMN IF NOT EXISTS summary TEXT,
    ADD COLUMN IF NOT EXISTS ai_summary TEXT,
    ADD COLUMN IF NOT EXISTS sentiment VARCHAR(40),
    ADD COLUMN IF NOT EXISTS intent VARCHAR(100),
    ADD COLUMN IF NOT EXISTS classification VARCHAR(100),
    ADD COLUMN IF NOT EXISTS next_best_action VARCHAR(255),
    ADD COLUMN IF NOT EXISTS last_message_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS metadata JSONB NOT NULL DEFAULT '{}'::jsonb;

ALTER TABLE conversation_thread
    DROP CONSTRAINT IF EXISTS ck_conversation_thread_channel;

ALTER TABLE conversation_thread
    ADD CONSTRAINT ck_conversation_thread_channel CHECK (
        channel IN ('WEB', 'WHATSAPP', 'EMAIL', 'SMS', 'VOICE', 'SOCIAL', 'OTHER')
    );

CREATE INDEX IF NOT EXISTS idx_conversation_thread_tenant_opportunity
    ON conversation_thread (tenant_id, opportunity_id)
    WHERE deleted_at IS NULL AND opportunity_id IS NOT NULL;

ALTER TABLE conversation_message
    ADD COLUMN IF NOT EXISTS direction VARCHAR(20) NOT NULL DEFAULT 'INBOUND',
    ADD COLUMN IF NOT EXISTS delivery_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
    ADD COLUMN IF NOT EXISTS content_type VARCHAR(30) NOT NULL DEFAULT 'TEXT',
    ADD COLUMN IF NOT EXISTS correlation_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS media_id UUID,
    ADD COLUMN IF NOT EXISTS media_url VARCHAR(1024),
    ADD COLUMN IF NOT EXISTS media_content_type VARCHAR(128),
    ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS failure_reason VARCHAR(1000),
    ADD COLUMN IF NOT EXISTS delivered_at TIMESTAMPTZ,
    ADD COLUMN IF NOT EXISTS read_at TIMESTAMPTZ;

ALTER TABLE conversation_message
    DROP CONSTRAINT IF EXISTS ck_conversation_message_direction;
ALTER TABLE conversation_message
    ADD CONSTRAINT ck_conversation_message_direction CHECK (direction IN ('INBOUND', 'OUTBOUND'));

ALTER TABLE conversation_message
    DROP CONSTRAINT IF EXISTS ck_conversation_message_delivery_status;
ALTER TABLE conversation_message
    ADD CONSTRAINT ck_conversation_message_delivery_status CHECK (
        delivery_status IN ('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED')
    );

ALTER TABLE conversation_message
    DROP CONSTRAINT IF EXISTS ck_conversation_message_content_type;
ALTER TABLE conversation_message
    ADD CONSTRAINT ck_conversation_message_content_type CHECK (
        content_type IN ('TEXT', 'MEDIA', 'FILE', 'SYSTEM')
    );

CREATE INDEX IF NOT EXISTS idx_conversation_message_correlation
    ON conversation_message (tenant_id, correlation_id)
    WHERE correlation_id IS NOT NULL;

CREATE TABLE IF NOT EXISTS conversation_participant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    conversation_id UUID NOT NULL REFERENCES conversation_thread (id),
    role VARCHAR(30) NOT NULL,
    participant_id UUID,
    display_name VARCHAR(255),
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    left_at TIMESTAMPTZ,
    created_by UUID,
    CONSTRAINT ck_conversation_participant_role CHECK (
        role IN ('CUSTOMER', 'AGENT', 'AI', 'SYSTEM', 'OBSERVER')
    )
);

CREATE INDEX IF NOT EXISTS idx_conversation_participant_conversation
    ON conversation_participant (tenant_id, conversation_id);

CREATE UNIQUE INDEX IF NOT EXISTS uq_conversation_participant_active
    ON conversation_participant (tenant_id, conversation_id, role, participant_id)
    WHERE left_at IS NULL;

CREATE TABLE IF NOT EXISTS conversation_attachment (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    conversation_id UUID NOT NULL REFERENCES conversation_thread (id),
    message_id UUID NOT NULL REFERENCES conversation_message (id),
    media_id UUID NOT NULL,
    file_name VARCHAR(255),
    content_type VARCHAR(128),
    size_bytes BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID
);

CREATE INDEX IF NOT EXISTS idx_conversation_attachment_message
    ON conversation_attachment (tenant_id, message_id);

CREATE TABLE IF NOT EXISTS conversation_timeline_entry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    conversation_id UUID NOT NULL REFERENCES conversation_thread (id),
    entry_type VARCHAR(60) NOT NULL,
    summary VARCHAR(500) NOT NULL,
    actor_id UUID,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    details JSONB NOT NULL DEFAULT '{}'::jsonb
);

CREATE INDEX IF NOT EXISTS idx_conversation_timeline_conversation
    ON conversation_timeline_entry (tenant_id, conversation_id, occurred_at DESC);
