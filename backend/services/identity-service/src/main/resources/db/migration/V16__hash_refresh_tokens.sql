-- Store only SHA-256 digests of opaque refresh tokens (never plaintext).
CREATE EXTENSION IF NOT EXISTS pgcrypto;

UPDATE refresh_tokens
SET token = encode(digest(convert_to(token, 'UTF8'), 'sha256'), 'hex')
WHERE token !~ '^[0-9a-f]{64}$';
