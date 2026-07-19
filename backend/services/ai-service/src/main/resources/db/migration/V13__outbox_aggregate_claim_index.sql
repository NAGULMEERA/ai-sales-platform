-- Speeds per-aggregate outbox claim gating (NOT EXISTS earlier PENDING/DISPATCHING/FAILED).
CREATE INDEX IF NOT EXISTS idx_outbox_events_aggregate_created
    ON outbox_events (aggregate_type, aggregate_id, created_at);
