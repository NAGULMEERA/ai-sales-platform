-- Upgrades the existing idx_outbox_events_aggregate (aggregate_type, aggregate_id) index with a
-- covering index that also includes created_at, matching ai-service's
-- V13__outbox_aggregate_claim_index.sql. The shared claimPendingEvents() query (common-events)
-- filters its NOT EXISTS subquery on aggregate_type + aggregate_id + created_at together; the
-- 2-column index only serves the equality predicates and still requires a filter pass over
-- created_at. The old 2-column index is left in place (additive change only).
CREATE INDEX IF NOT EXISTS idx_outbox_events_aggregate_created
    ON outbox_events (aggregate_type, aggregate_id, created_at);
