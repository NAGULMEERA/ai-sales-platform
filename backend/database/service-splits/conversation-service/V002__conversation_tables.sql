-- From DB.html v7 section 4
CREATE TABLE conversations (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    lead_id UUID,
    customer_profile_id UUID,
    channel TEXT,
    message TEXT,
    sender TEXT,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conversations_lead_created ON conversations(lead_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_conversations_tenant_created ON conversations(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_conversations_sender ON conversations(sender);
-- skipped (non-immutable): idx_conversations_recent on conversations
CREATE INDEX IF NOT EXISTS idx_conversations_tenant_customer_created ON conversations(tenant_id, customer_profile_id, created_at DESC);

-- DB.html 4.2 conversation_state
CREATE TABLE conversation_state (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID,
    current_step TEXT,
    state_data JSONB DEFAULT '{}'::jsonb,
    last_activity TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conversation_state_lead ON conversation_state(lead_id);
CREATE INDEX IF NOT EXISTS idx_conversation_state_activity ON conversation_state(last_activity);

-- DB.html 4.3 conversation_history
CREATE TABLE conversation_history (
    id BIGSERIAL PRIMARY KEY,
    lead_id UUID,
    turn_number INTEGER,
    speaker TEXT,
    message TEXT,
    intent TEXT,
    entities JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conversation_history_lead_turn ON conversation_history(lead_id, turn_number);

-- DB.html 4.4 conversation_memory
CREATE TABLE conversation_memory (
    id BIGSERIAL PRIMARY KEY,
    lead_id UUID,
    embedding vector(1536),
    context TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conversation_memory_vector ON conversation_memory(embedding vector_cosine_ops HNSW);

-- DB.html 4.5 ai_sessions
CREATE TABLE ai_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID,
    state TEXT,
    paused_by UUID,
    paused_at TIMESTAMPTZ,
    resumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_ai_sessions_lead ON ai_sessions(lead_id);
CREATE INDEX IF NOT EXISTS idx_ai_sessions_state ON ai_sessions(state);

-- DB.html 4.6 conversation_transfers
CREATE TABLE conversation_transfers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID,
    from_agent_type TEXT,
    to_agent_type TEXT,
    reason TEXT,
    transferred_by UUID,
    transferred_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_conversation_transfers_lead ON conversation_transfers(lead_id);
CREATE INDEX IF NOT EXISTS idx_conversation_transfers_at ON conversation_transfers(transferred_at);

-- DB.html 4.7 conversation_summaries
CREATE TABLE conversation_summaries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID,
    summary TEXT,
    key_points JSONB DEFAULT '{}'::jsonb,
    sentiment_score DECIMAL(19,4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conversation_summaries_lead ON conversation_summaries(lead_id);

-- DB.html 4.8 conversation_sentiment
CREATE TABLE conversation_sentiment (
    id BIGSERIAL PRIMARY KEY,
    lead_id UUID,
    turn_number INTEGER,
    speaker TEXT,
    sentiment_score DECIMAL(19,4),
    sentiment_label TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conversation_sentiment_lead ON conversation_sentiment(lead_id, created_at DESC);
