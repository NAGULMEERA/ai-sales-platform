-- Token families support refresh rotation, reuse detection, and multi-device sessions.
ALTER TABLE refresh_tokens ADD COLUMN IF NOT EXISTS token_family_id UUID;

UPDATE refresh_tokens SET token_family_id = id WHERE token_family_id IS NULL;

ALTER TABLE refresh_tokens ALTER COLUMN token_family_id SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_refresh_tokens_token_family ON refresh_tokens(token_family_id);

-- At most one unconsumed verification token per user (enforced at application layer; index aids lookup).
CREATE INDEX IF NOT EXISTS idx_email_verification_active_user
    ON email_verification_tokens(user_id)
    WHERE consumed_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_password_reset_active_user
    ON password_reset_tokens(user_id)
    WHERE consumed_at IS NULL;
