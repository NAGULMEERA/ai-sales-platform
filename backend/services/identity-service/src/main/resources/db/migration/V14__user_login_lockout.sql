-- Timed account lockout after repeated failed password attempts.

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS failed_login_attempts INT NOT NULL DEFAULT 0;

ALTER TABLE users
    ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP NULL;

CREATE INDEX IF NOT EXISTS idx_users_locked_until ON users (locked_until)
    WHERE locked_until IS NOT NULL;
