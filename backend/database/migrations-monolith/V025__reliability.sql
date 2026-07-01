-- ============================================================================
-- V025__reliability.sql
-- Generated from DB.html v7.0: Reliability idempotency/circuit breaker (§15.2,15.5,15.6)
-- Lab monolith only. Split per service for production (UUID refs, no cross-FK).
-- ============================================================================

-- DB.html 15.2 idempotency_keys
CREATE TABLE idempotency_keys (
    key TEXT PRIMARY KEY,
    tenant_id UUID NOT NULL,
    response JSONB DEFAULT '{}'::jsonb,
    expires_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 15.5 retry_logs
CREATE TABLE retry_logs (
    id BIGSERIAL PRIMARY KEY,
    operation TEXT,
    attempt INTEGER,
    error_message TEXT,
    next_retry_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 15.6 circuit_breaker_state
CREATE TABLE circuit_breaker_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    service TEXT,
    state TEXT,
    failure_count INTEGER,
    last_failure_at TIMESTAMPTZ,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

