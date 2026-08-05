-- UserSessionRepository.findByRefreshTokenIdAndRevokedAtIsNull (used on every refresh-token
-- rotation/revocation, including RefreshTokenService.revokeTokenFamily's batched lookup) filters
-- on refresh_token_id + revoked_at IS NULL. No index existed on refresh_token_id at all
-- (V5__sessions_verification_reset.sql only indexes user_id and expires_at), forcing a full
-- table scan on every call. A partial index matching the exact predicate keeps the index small
-- and always relevant, since revoked sessions are never queried by this method.
CREATE INDEX IF NOT EXISTS idx_user_sessions_refresh_token_active
    ON user_sessions (refresh_token_id)
    WHERE revoked_at IS NULL;
