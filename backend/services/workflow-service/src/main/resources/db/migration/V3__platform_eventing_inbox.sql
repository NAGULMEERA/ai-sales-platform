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
    error_class VARCHAR(255),
    retry_count INT NOT NULL DEFAULT 0,
    consumer_name VARCHAR(100) NOT NULL,
    last_attempt_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_dead_letter_consumer ON dead_letter(consumer_name, created_at);

CREATE TABLE IF NOT EXISTS outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    topic VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    published_at TIMESTAMP,
    retry_count INT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status_created ON outbox_events(status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_events_aggregate ON outbox_events(aggregate_type, aggregate_id);
