CREATE TABLE tenant_subscriptions (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL UNIQUE REFERENCES tenants(id) ON DELETE CASCADE,
    plan VARCHAR(20) NOT NULL DEFAULT 'FREE',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    payment_provider VARCHAR(50),
    external_subscription_id VARCHAR(255),
    trial_ends_at TIMESTAMP,
    current_period_end TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenant_subscriptions_plan ON tenant_subscriptions(plan);
CREATE INDEX idx_tenant_subscriptions_status ON tenant_subscriptions(status);

CREATE TABLE subscription_features (
    id UUID PRIMARY KEY,
    plan VARCHAR(20) NOT NULL,
    feature_code VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    limit_value BIGINT,
    UNIQUE (plan, feature_code)
);

INSERT INTO subscription_features (id, plan, feature_code, enabled, limit_value) VALUES
    (gen_random_uuid(), 'FREE', 'ai.assistant', TRUE, 100),
    (gen_random_uuid(), 'FREE', 'lead.import', TRUE, 500),
    (gen_random_uuid(), 'FREE', 'workflow.automation', FALSE, NULL),
    (gen_random_uuid(), 'FREE', 'analytics.advanced', FALSE, NULL),
    (gen_random_uuid(), 'PREMIUM', 'ai.assistant', TRUE, NULL),
    (gen_random_uuid(), 'PREMIUM', 'lead.import', TRUE, NULL),
    (gen_random_uuid(), 'PREMIUM', 'workflow.automation', TRUE, NULL),
    (gen_random_uuid(), 'PREMIUM', 'analytics.advanced', TRUE, NULL);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    user_id UUID,
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(100),
    resource_id VARCHAR(255),
    ip_address VARCHAR(45),
    user_agent TEXT,
    details_json TEXT,
    correlation_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_tenant_id ON audit_logs(tenant_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
