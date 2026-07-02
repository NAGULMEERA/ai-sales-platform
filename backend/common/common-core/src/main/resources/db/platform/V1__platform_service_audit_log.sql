-- Platform Infrastructure Epic — Phase 3
-- Copy into each microservice schema (database-per-service).
-- Standard local audit trail before audit-service integration.

CREATE TABLE IF NOT EXISTS service_audit_log (
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

CREATE INDEX IF NOT EXISTS idx_service_audit_log_tenant ON service_audit_log(tenant_id, created_at);
CREATE INDEX IF NOT EXISTS idx_service_audit_log_resource ON service_audit_log(resource_type, resource_id);
