-- ============================================================================
-- V032__agent_orchestration_gap.sql
-- Generated from DB.html v7.0: Agent orchestration gap vs DDS (§25)
-- Lab monolith only. Split per service for production (UUID refs, no cross-FK).
-- ============================================================================

-- DB.html 25.1 agent_plans
CREATE TABLE IF NOT EXISTS agent_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_execution_id UUID,
    plan_type TEXT,
    plan_steps JSONB DEFAULT '{}'::jsonb,
    current_step_index INTEGER,
    status TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_plans_execution ON agent_plans(agent_execution_id);
CREATE INDEX IF NOT EXISTS idx_agent_plans_status ON agent_plans(status);

-- DB.html 25.3 tool_calls
CREATE TABLE IF NOT EXISTS tool_calls (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_plan_id UUID,
    tool_registry_id UUID,
    tool_call_id TEXT,
    parameters JSONB DEFAULT '{}'::jsonb,
    result JSONB DEFAULT '{}'::jsonb,
    status TEXT,
    execution_time_ms INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tool_calls_plan ON tool_calls(agent_plan_id);
CREATE INDEX IF NOT EXISTS idx_tool_calls_tool ON tool_calls(tool_registry_id);
CREATE INDEX IF NOT EXISTS idx_tool_calls_status ON tool_calls(status);

-- DB.html 25.4 agent_memory
CREATE TABLE IF NOT EXISTS agent_memory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_execution_id UUID,
    memory_type TEXT,
    memory_key TEXT,
    memory_value JSONB DEFAULT '{}'::jsonb,
    embedding vector(1536),
    ttl_seconds INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_agent_memory_execution ON agent_memory(agent_execution_id);
CREATE INDEX IF NOT EXISTS idx_agent_memory_type ON agent_memory(memory_type);
CREATE INDEX IF NOT EXISTS idx_agent_memory_vector ON agent_memory(embedding vector_cosine_ops HNSW);
CREATE INDEX IF NOT EXISTS idx_agent_memory_expires ON agent_memory(expires_at) WHERE expires_at IS NOT NULL;

-- DB.html 25.5 agent_feedback
CREATE TABLE IF NOT EXISTS agent_feedback (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_execution_id UUID,
    feedback_type TEXT,
    feedback_value DECIMAL(19,4),
    feedback_text TEXT,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_agent_feedback_execution ON agent_feedback(agent_execution_id);
CREATE INDEX IF NOT EXISTS idx_agent_feedback_type ON agent_feedback(feedback_type);
