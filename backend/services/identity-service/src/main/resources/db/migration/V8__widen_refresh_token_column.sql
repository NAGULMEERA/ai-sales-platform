-- Refresh tokens are opaque strings; widen column for legacy JWT rows if any exist.
ALTER TABLE refresh_tokens ALTER COLUMN token TYPE VARCHAR(2048);
