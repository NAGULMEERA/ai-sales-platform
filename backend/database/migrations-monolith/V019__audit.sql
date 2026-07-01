-- ============================================================================
-- V019__audit.sql
-- Generated from DB.html v7.0: Audit and data governance (§16-17)
-- Lab monolith only. Split per service for production (UUID refs, no cross-FK).
-- ============================================================================

-- DB.html 16.1 audit_log
CREATE TABLE audit_log (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    user_id UUID,
    action TEXT,
    resource_type TEXT,
    resource_id TEXT,
    old_values JSONB DEFAULT '{}'::jsonb,
    new_values JSONB DEFAULT '{}'::jsonb,
    ip_address INET,
    user_agent TEXT,
    correlation_id TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_tenant_created ON audit_log(tenant_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_resource ON audit_log(resource_type, resource_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_correlation_id ON audit_log(correlation_id);

-- DB.html 16.2 audit_hash_chain
CREATE TABLE audit_hash_chain (
    id BIGSERIAL PRIMARY KEY,
    audit_log_id BIGINT,
    previous_hash TEXT,
    current_hash TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 16.3 legal_holds
CREATE TABLE legal_holds (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID,
    case_reference TEXT,
    hold_reason TEXT,
    requested_by UUID,
    approved_by UUID,
    hold_start TIMESTAMPTZ,
    hold_end TIMESTAMPTZ,
    status TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 16.4 legal_hold_records
CREATE TABLE legal_hold_records (
    id BIGSERIAL PRIMARY KEY,
    legal_hold_id UUID,
    table_name TEXT,
    record_id UUID,
    record_data JSONB DEFAULT '{}'::jsonb,
    preserved_at TIMESTAMPTZ
);


-- DB.html 17.1 data_classification
CREATE TABLE data_classification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    table_name TEXT,
    column_name TEXT,
    classification TEXT,
    justification TEXT
);

CREATE INDEX IF NOT EXISTS idx_data_classification_table ON data_classification(table_name);

-- DB.html 17.2 data_access_policies
CREATE TABLE data_access_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    role TEXT,
    table_name TEXT,
    operation TEXT,
    filter_condition TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 17.3 data_archival_policies
CREATE TABLE data_archival_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    table_name TEXT,
    hot_retention_days INTEGER,
    warm_retention_days INTEGER,
    cold_retention_days INTEGER,
    enabled BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 17.4 archival_logs
CREATE TABLE archival_logs (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    table_name TEXT,
    archived_count INTEGER,
    tier TEXT,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    status TEXT,
    error_message TEXT
);


-- DB.html 17.5 retention_policies
CREATE TABLE retention_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    data_type TEXT,
    retention_days INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_retention_policies_tenant ON retention_policies(tenant_id);
