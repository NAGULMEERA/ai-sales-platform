-- Sprint C: split EXECUTE/EMBED daily budgets + Kafka inbox for plan-linked quota packages.

ALTER TABLE tenant_ai_quota
    ADD COLUMN IF NOT EXISTS daily_execute_token_limit BIGINT,
    ADD COLUMN IF NOT EXISTS daily_embed_token_limit BIGINT,
    ADD COLUMN IF NOT EXISTS plan_code VARCHAR(50);

ALTER TABLE tenant_ai_quota
    DROP CONSTRAINT IF EXISTS ck_tenant_ai_quota_positive;

ALTER TABLE tenant_ai_quota
    ADD CONSTRAINT ck_tenant_ai_quota_positive CHECK (daily_token_limit > 0);

ALTER TABLE tenant_ai_quota
    ADD CONSTRAINT ck_tenant_ai_quota_execute_nonneg CHECK (
        daily_execute_token_limit IS NULL OR daily_execute_token_limit >= 0
    );

ALTER TABLE tenant_ai_quota
    ADD CONSTRAINT ck_tenant_ai_quota_embed_nonneg CHECK (
        daily_embed_token_limit IS NULL OR daily_embed_token_limit >= 0
    );

CREATE TABLE IF NOT EXISTS processed_events (
    event_id VARCHAR(255) NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, consumer_name)
);

CREATE INDEX IF NOT EXISTS idx_processed_events_consumer ON processed_events(consumer_name, processed_at);

CREATE TABLE IF NOT EXISTS dead_letter (
    id UUID PRIMARY KEY,
    topic VARCHAR(255) NOT NULL,
    partition_id INT,
    message_offset BIGINT,
    event_id VARCHAR(255),
    event_type VARCHAR(100),
    payload TEXT NOT NULL,
    error_message TEXT,
    consumer_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dead_letter_consumer ON dead_letter(consumer_name, created_at);
