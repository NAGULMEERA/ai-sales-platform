-- Platform Infrastructure Epic — Phase 1: inbox idempotency + dead letter.

CREATE TABLE processed_events (
    event_id VARCHAR(255) NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, consumer_name)
);

CREATE INDEX idx_processed_events_consumer ON processed_events(consumer_name, processed_at);

CREATE TABLE dead_letter (
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

CREATE INDEX idx_dead_letter_consumer ON dead_letter(consumer_name, created_at);
