-- ============================================================================
-- V024__eventing.sql
-- Generated from DB.html v7.0: Eventing outbox/inbox/DLQ (§15.1,15.3,15.4)
-- Lab monolith only. Split per service for production (UUID refs, no cross-FK).
-- ============================================================================

-- DB.html 15.1 outbox_events
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    aggregate_type TEXT,
    aggregate_id TEXT,
    event_type TEXT,
    payload JSONB DEFAULT '{}'::jsonb,
    status TEXT,
    retry_count INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status ON outbox_events(status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_events_aggregate ON outbox_events(aggregate_type, aggregate_id);

-- DB.html 15.3 processed_events
CREATE TABLE processed_events (
    event_id TEXT PRIMARY KEY,
    consumer_name TEXT,
    processed_at TIMESTAMPTZ
);


-- DB.html 15.4 dead_letter
CREATE TABLE dead_letter (
    id BIGSERIAL PRIMARY KEY,
    topic TEXT,
    partition INTEGER,
    msg_offset BIGINT,
    payload JSONB DEFAULT '{}'::jsonb,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

