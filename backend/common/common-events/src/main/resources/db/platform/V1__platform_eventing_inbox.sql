-- Platform Infrastructure Epic — Phase 1
-- Copy into each microservice schema (database-per-service).
-- Outbox table may already exist; this adds inbox + dead letter only.

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
