-- Idempotency keys for safe retries on tenant creation (Story C).

CREATE TABLE idempotency_keys (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL UNIQUE,
    operation VARCHAR(100) NOT NULL,
    resource_id UUID,
    response_body TEXT NOT NULL,
    http_status INT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_idempotency_keys_expires ON idempotency_keys(expires_at);
