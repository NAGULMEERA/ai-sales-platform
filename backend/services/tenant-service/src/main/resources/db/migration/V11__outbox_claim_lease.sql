ALTER TABLE outbox_events ADD COLUMN IF NOT EXISTS claimed_at TIMESTAMP;
CREATE INDEX IF NOT EXISTS idx_outbox_events_dispatching_claimed ON outbox_events(status, claimed_at);
