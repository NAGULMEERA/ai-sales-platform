-- Idempotent Stripe webhook processing (event_id is Stripe's evt_...).
CREATE TABLE stripe_webhook_event (
    event_id VARCHAR(255) PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_stripe_webhook_event_processed_at ON stripe_webhook_event (processed_at);

COMMENT ON TABLE stripe_webhook_event IS 'Dedup store for Stripe webhook deliveries';
