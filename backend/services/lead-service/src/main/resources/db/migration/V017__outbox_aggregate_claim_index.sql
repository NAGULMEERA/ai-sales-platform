-- Speeds per-aggregate outbox claim gating (NOT EXISTS earlier PENDING/DISPATCHING/FAILED).
-- Mirrors ai-service's V13__outbox_aggregate_claim_index.sql; this service's OutboxRepository
-- shares the same claimPendingEvents() query (common-events) and had no supporting index.
CREATE INDEX IF NOT EXISTS idx_outbox_events_aggregate_created
    ON outbox_events (aggregate_type, aggregate_id, created_at);
