-- Local audit trail for tenant lifecycle mutations (Story C).

CREATE TABLE tenant_audit_log (
    id UUID PRIMARY KEY,
    tenant_id UUID,
    user_id VARCHAR(255),
    action VARCHAR(100) NOT NULL,
    resource_type VARCHAR(50) NOT NULL,
    resource_id VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(255),
    details_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenant_audit_log_tenant ON tenant_audit_log(tenant_id, created_at);
CREATE INDEX idx_tenant_audit_log_resource ON tenant_audit_log(resource_type, resource_id);
