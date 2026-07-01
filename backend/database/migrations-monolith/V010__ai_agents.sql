-- ============================================================================
-- V010__ai_agents.sql
-- AI Agents: Agents, Tools, Memories, Executions
-- ============================================================================

-- ============================================================================
-- agents
-- ============================================================================
CREATE TABLE agents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    agent_type VARCHAR(50) NOT NULL,
    description TEXT,
    config JSONB DEFAULT '{}'::jsonb,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_agents_tenant ON agents(tenant_id);
CREATE INDEX idx_agents_type ON agents(agent_type);

-- ============================================================================
-- agent_tools
-- ============================================================================
CREATE TABLE agent_tools (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id UUID NOT NULL,
    tool_name VARCHAR(100) NOT NULL,
    tool_description TEXT,
    tool_parameters JSONB DEFAULT '{}'::jsonb,
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_tools_agent ON agent_tools(agent_id);

-- ============================================================================
-- agent_memories
-- ============================================================================
CREATE TABLE agent_memories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_execution_id UUID,
    memory_type VARCHAR(50) NOT NULL,
    memory_key VARCHAR(255) NOT NULL,
    memory_value JSONB NOT NULL,
    embedding VECTOR(1536),
    ttl_seconds INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMPTZ
);

CREATE INDEX idx_agent_memories_execution ON agent_memories(agent_execution_id);
CREATE INDEX idx_agent_memories_type ON agent_memories(memory_type);

-- ============================================================================
-- agent_tasks
-- ============================================================================
CREATE TABLE agent_tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_id UUID NOT NULL,
    task_type VARCHAR(50) NOT NULL,
    task_data JSONB DEFAULT '{}'::jsonb,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    priority INTEGER DEFAULT 5,
    scheduled_at TIMESTAMPTZ,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_agent_tasks_agent ON agent_tasks(agent_id);
CREATE INDEX idx_agent_tasks_status ON agent_tasks(status);
CREATE INDEX idx_agent_tasks_scheduled ON agent_tasks(scheduled_at);

-- ============================================================================
-- agent_executions
-- ============================================================================
CREATE TABLE agent_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_task_id UUID,
    agent_type VARCHAR(50) NOT NULL,
    lead_id UUID,
    status VARCHAR(50) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    retry_count INTEGER DEFAULT 0,
    max_retries INTEGER DEFAULT 3,
    timeout_seconds INTEGER DEFAULT 30,
    error_message TEXT,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_agent_executions_task ON agent_executions(agent_task_id);
CREATE INDEX idx_agent_executions_lead ON agent_executions(lead_id);
CREATE INDEX idx_agent_executions_status ON agent_executions(status);

-- ============================================================================
-- tool_registry
-- ============================================================================
CREATE TABLE tool_registry (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tool_name VARCHAR(100) NOT NULL,
    tool_description TEXT NOT NULL,
    tool_parameters JSONB DEFAULT '{}'::jsonb,
    service_class VARCHAR(255) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE tool_registry ADD CONSTRAINT uk_tool_registry_name UNIQUE (tool_name);

-- ============================================================================
-- tool_executions
-- ============================================================================
CREATE TABLE tool_executions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tool_registry_id UUID NOT NULL,
    agent_execution_id UUID,
    parameters JSONB DEFAULT '{}'::jsonb,
    result JSONB DEFAULT '{}'::jsonb,
    status VARCHAR(50) NOT NULL,
    execution_time_ms INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tool_executions_tool ON tool_executions(tool_registry_id);
CREATE INDEX idx_tool_executions_agent ON tool_executions(agent_execution_id);
CREATE INDEX idx_tool_executions_status ON tool_executions(status);

-- ============================================================================
-- agent_errors
-- ============================================================================
CREATE TABLE agent_errors (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    agent_execution_id UUID NOT NULL,
    error_code VARCHAR(100),
    error_message TEXT,
    error_stack TEXT,
    recovered BOOLEAN DEFAULT false,
    recovery_action TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agent_errors_execution ON agent_errors(agent_execution_id);

-- ============================================================================
-- Foreign Key Constraints
-- ============================================================================
ALTER TABLE agents ADD CONSTRAINT fk_agents_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE agent_tools ADD CONSTRAINT fk_agent_tools_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
ALTER TABLE agent_tasks ADD CONSTRAINT fk_agent_tasks_agent FOREIGN KEY (agent_id) REFERENCES agents(id) ON DELETE CASCADE;
ALTER TABLE agent_executions ADD CONSTRAINT fk_agent_executions_task FOREIGN KEY (agent_task_id) REFERENCES agent_tasks(id) ON DELETE SET NULL;
ALTER TABLE tool_executions ADD CONSTRAINT fk_tool_executions_tool FOREIGN KEY (tool_registry_id) REFERENCES tool_registry(id);
ALTER TABLE tool_executions ADD CONSTRAINT fk_tool_executions_agent FOREIGN KEY (agent_execution_id) REFERENCES agent_executions(id) ON DELETE CASCADE;
ALTER TABLE agent_errors ADD CONSTRAINT fk_agent_errors_execution FOREIGN KEY (agent_execution_id) REFERENCES agent_executions(id) ON DELETE CASCADE;
