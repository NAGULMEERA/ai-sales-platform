-- List conversations ordered by created_at (tenant-scoped soft-delete filter).
CREATE INDEX IF NOT EXISTS idx_conversation_thread_tenant_created
    ON conversation_thread (tenant_id, created_at DESC)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_conversation_attachment_tenant_conversation
    ON conversation_attachment (tenant_id, conversation_id, created_at);
