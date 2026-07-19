-- Align dead_letter with common-events DeadLetterMessage (identity/lead already have these).
ALTER TABLE dead_letter
    ADD COLUMN IF NOT EXISTS error_class VARCHAR(255);

ALTER TABLE dead_letter
    ADD COLUMN IF NOT EXISTS retry_count INT NOT NULL DEFAULT 0;

ALTER TABLE dead_letter
    ADD COLUMN IF NOT EXISTS last_attempt_at TIMESTAMP NOT NULL DEFAULT NOW();
