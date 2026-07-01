-- ============================================================================
-- V030__human_agent.sql
-- Generated from DB.html v7.0: Human agent workspace (§12)
-- Lab monolith only. Split per service for production (UUID refs, no cross-FK).
-- ============================================================================

-- DB.html 12.1 agent_queue
CREATE TABLE agent_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    lead_id UUID,
    reason TEXT,
    confidence_score INTEGER,
    assigned_to UUID,
    resolved BOOLEAN DEFAULT false,
    resolved_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_queue_tenant ON agent_queue(tenant_id);
CREATE INDEX IF NOT EXISTS idx_agent_queue_resolved ON agent_queue(resolved);

-- DB.html 12.2 agent_assignments
CREATE TABLE agent_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID,
    assigned_to UUID,
    assigned_at TIMESTAMPTZ,
    unassigned_at TIMESTAMPTZ
);


-- DB.html 12.3 agent_notes
CREATE TABLE agent_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID,
    note TEXT,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 12.4 agent_activity
CREATE TABLE agent_activity (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    activity_type TEXT,
    activity_data JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 12.5 agent_availability
CREATE TABLE agent_availability (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID,
    day_of_week INTEGER,
    start_time TEXT,
    end_time TEXT,
    status TEXT,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

