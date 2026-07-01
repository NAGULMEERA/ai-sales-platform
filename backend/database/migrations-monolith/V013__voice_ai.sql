-- ============================================================================
-- V013__voice_ai.sql
-- Voice AI: Calls, Recordings, Transcripts
-- ============================================================================

-- ============================================================================
-- call_sessions
-- ============================================================================
CREATE TABLE call_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    lead_id UUID,
    call_sid VARCHAR(255) NOT NULL,
    from_number VARCHAR(50) NOT NULL,
    to_number VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    duration_seconds INTEGER,
    recording_url TEXT,
    transcript TEXT,
    started_at TIMESTAMPTZ,
    ended_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_call_sessions_tenant ON call_sessions(tenant_id, created_at DESC);
CREATE INDEX idx_call_sessions_lead ON call_sessions(lead_id);
CREATE INDEX idx_call_sessions_sid ON call_sessions(call_sid);

-- ============================================================================
-- call_events
-- ============================================================================
CREATE TABLE call_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    call_session_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_call_events_session ON call_events(call_session_id, created_at);

-- ============================================================================
-- call_recordings
-- ============================================================================
CREATE TABLE call_recordings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    call_session_id UUID NOT NULL,
    recording_url TEXT NOT NULL,
    duration_seconds INTEGER,
    file_size BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_call_recordings_session ON call_recordings(call_session_id);

-- ============================================================================
-- call_transcripts
-- ============================================================================
CREATE TABLE call_transcripts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    call_session_id UUID NOT NULL,
    transcript TEXT NOT NULL,
    language VARCHAR(10),
    confidence DECIMAL(3,2),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_call_transcripts_session ON call_transcripts(call_session_id);

-- ============================================================================
-- voice_call_logs
-- ============================================================================
CREATE TABLE voice_call_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    lead_id UUID,
    call_sid VARCHAR(255) NOT NULL,
    direction VARCHAR(20) NOT NULL,
    status VARCHAR(50) NOT NULL,
    cost DECIMAL(19,6),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_voice_call_logs_tenant ON voice_call_logs(tenant_id, created_at DESC);

-- ============================================================================
-- Foreign Key Constraints
-- ============================================================================
ALTER TABLE call_sessions ADD CONSTRAINT fk_call_sessions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE call_events ADD CONSTRAINT fk_call_events_session FOREIGN KEY (call_session_id) REFERENCES call_sessions(id) ON DELETE CASCADE;
ALTER TABLE call_recordings ADD CONSTRAINT fk_call_recordings_session FOREIGN KEY (call_session_id) REFERENCES call_sessions(id) ON DELETE CASCADE;
ALTER TABLE call_transcripts ADD CONSTRAINT fk_call_transcripts_session FOREIGN KEY (call_session_id) REFERENCES call_sessions(id) ON DELETE CASCADE;
ALTER TABLE voice_call_logs ADD CONSTRAINT fk_voice_call_logs_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
