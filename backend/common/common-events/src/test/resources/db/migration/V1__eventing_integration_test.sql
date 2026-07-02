-- Integration test schema: outbox + inbox (Platform Infrastructure Epic Phase 4)

CREATE TABLE outbox_events (
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

CREATE INDEX idx_outbox_events_status_created ON outbox_events(status, created_at);

CREATE TABLE processed_events (
    event_id VARCHAR(255) NOT NULL,
    consumer_name VARCHAR(100) NOT NULL,
    processed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    PRIMARY KEY (event_id, consumer_name)
);

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
