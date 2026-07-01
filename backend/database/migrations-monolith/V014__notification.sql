-- ============================================================================
-- V014__notification.sql
-- Notification: Notifications, Templates, Queue, History
-- ============================================================================

-- ============================================================================
-- notifications
-- ============================================================================
CREATE TABLE notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    lead_id UUID,
    user_id UUID,
    channel VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    subject VARCHAR(255),
    content TEXT,
    template_id UUID,
    variables JSONB DEFAULT '{}'::jsonb,
    status notification_status NOT NULL DEFAULT 'PENDING',
    priority INTEGER DEFAULT 5,
    scheduled_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    read_at TIMESTAMPTZ,
    retry_count INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_notifications_tenant ON notifications(tenant_id, created_at DESC);
CREATE INDEX idx_notifications_lead ON notifications(lead_id);
CREATE INDEX idx_notifications_user ON notifications(user_id);
CREATE INDEX idx_notifications_status ON notifications(status);
CREATE INDEX idx_notifications_scheduled ON notifications(scheduled_at) WHERE scheduled_at IS NOT NULL;

-- ============================================================================
-- notification_templates
-- ============================================================================
CREATE TABLE notification_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    template TEXT NOT NULL,
    variables JSONB DEFAULT '{}'::jsonb,
    version INTEGER DEFAULT 1,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_notification_templates_tenant ON notification_templates(tenant_id);
CREATE INDEX idx_notification_templates_channel ON notification_templates(channel);

-- ============================================================================
-- notification_queue
-- ============================================================================
CREATE TABLE notification_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL,
    channel VARCHAR(50) NOT NULL,
    payload JSONB NOT NULL,
    priority INTEGER DEFAULT 5,
    status notification_status NOT NULL DEFAULT 'PENDING',
    attempts INTEGER DEFAULT 0,
    next_attempt_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_queue_notification ON notification_queue(notification_id);
CREATE INDEX idx_notification_queue_status ON notification_queue(status);
CREATE INDEX idx_notification_queue_next_attempt ON notification_queue(next_attempt_at) WHERE next_attempt_at IS NOT NULL;

-- ============================================================================
-- notification_history
-- ============================================================================
CREATE TABLE notification_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    notification_id UUID NOT NULL,
    channel VARCHAR(50) NOT NULL,
    status notification_status NOT NULL,
    message TEXT,
    response JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notification_history_notification ON notification_history(notification_id, created_at DESC);

-- ============================================================================
-- in_app_notifications
-- ============================================================================
CREATE TABLE in_app_notifications (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50),
    action_url VARCHAR(255),
    is_read BOOLEAN DEFAULT false,
    read_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_in_app_notifications_user ON in_app_notifications(user_id, created_at DESC);
CREATE INDEX idx_in_app_notifications_read ON in_app_notifications(user_id, is_read);

-- ============================================================================
-- Foreign Key Constraints
-- ============================================================================
ALTER TABLE notifications ADD CONSTRAINT fk_notifications_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE notification_templates ADD CONSTRAINT fk_notification_templates_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE notification_queue ADD CONSTRAINT fk_notification_queue_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE;
ALTER TABLE notification_history ADD CONSTRAINT fk_notification_history_notification FOREIGN KEY (notification_id) REFERENCES notifications(id) ON DELETE CASCADE;
ALTER TABLE in_app_notifications ADD CONSTRAINT fk_in_app_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
