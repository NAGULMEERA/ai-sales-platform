-- ============================================================================
-- V028__advanced_indexes.sql
-- Advanced Indexes: HNSW, GIN, Partial, Covering
-- ============================================================================

-- ============================================================================
-- HNSW Vector Indexes (pgvector)
-- ============================================================================
CREATE INDEX idx_catalog_embeddings_hnsw ON catalog_embeddings USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_document_embeddings_hnsw ON document_embeddings USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_semantic_cache_hnsw ON semantic_cache USING hnsw (query_embedding vector_cosine_ops);
CREATE INDEX idx_conversation_memory_hnsw ON conversation_memory USING hnsw (embedding vector_cosine_ops);
CREATE INDEX idx_agent_memory_hnsw ON agent_memories USING hnsw (embedding vector_cosine_ops);

-- ============================================================================
-- GIN Indexes for JSONB
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_leads_metadata_gin ON leads USING gin (metadata);
CREATE INDEX IF NOT EXISTS idx_customers_metadata_gin ON customers USING gin (metadata);
CREATE INDEX IF NOT EXISTS idx_workflow_executions_gin ON workflow_executions USING gin (execution_data);
CREATE INDEX IF NOT EXISTS idx_workflow_variables_gin ON workflow_variables USING gin (variable_value);
CREATE INDEX IF NOT EXISTS idx_properties_attributes_gin ON properties USING gin (attributes);
CREATE INDEX IF NOT EXISTS idx_properties_location_gin ON properties USING gin (location);

-- ============================================================================
-- Partial Indexes
-- ============================================================================
CREATE INDEX idx_properties_active ON properties(tenant_id, status) WHERE deleted_at IS NULL AND status = 'ACTIVE';
CREATE INDEX idx_workflows_published ON workflows(tenant_id, name) WHERE status = 'PUBLISHED' AND deleted_at IS NULL;
CREATE INDEX idx_notifications_pending ON notifications(tenant_id, scheduled_at) WHERE status = 'PENDING';

-- ============================================================================
-- Covering Indexes
-- ============================================================================
CREATE INDEX idx_leads_cover ON leads(tenant_id, status) INCLUDE (score, assigned_to, created_at);
CREATE INDEX idx_appointments_cover ON appointments(tenant_id, start_time) INCLUDE (status, lead_id, sales_user_id);
CREATE INDEX idx_properties_cover ON properties(tenant_id, status) INCLUDE (price, bedrooms, sqft);
CREATE INDEX idx_llm_requests_cover ON llm_requests(tenant_id, provider) INCLUDE (model, cost, success);

-- ============================================================================
-- Full Text Search Indexes
-- ============================================================================
CREATE INDEX idx_leads_fts ON leads USING gin(to_tsvector('english', customer_name || ' ' || COALESCE(transcript, '')));
CREATE INDEX idx_properties_fts ON properties USING gin(to_tsvector('english', name || ' ' || COALESCE(description, '')));
CREATE INDEX idx_customers_fts ON customers USING gin(to_tsvector('english', first_name || ' ' || last_name));

-- ============================================================================
-- Composite Indexes with Functions
-- ============================================================================
CREATE INDEX idx_leads_date_qualified ON leads(tenant_id, (metadata->>'qualified_at')::timestamptz) WHERE status = 'QUALIFIED';
CREATE INDEX idx_appointments_date_tz ON appointments(tenant_id, date_trunc('day', start_time));
CREATE INDEX idx_ai_cost_tenant_date ON ai_cost_tracking(tenant_id, date_trunc('month', date));
