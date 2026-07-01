-- ============================================================================
-- V003__tenant.sql
-- Tenant: Tenants, Settings, Features, Limits
-- ============================================================================

-- ============================================================================
-- tenants
-- ============================================================================
CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    tenant_code VARCHAR(50) NOT NULL,
    industry VARCHAR(50) NOT NULL,
    subscription_tier VARCHAR(50) NOT NULL DEFAULT 'STARTER',
    status tenant_status NOT NULL DEFAULT 'TRIAL',
    config JSONB DEFAULT '{}'::jsonb,
    trial_start TIMESTAMPTZ,
    trial_end TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ NULL,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_tenants_tenant_code ON tenants(tenant_code);
CREATE INDEX idx_tenants_status ON tenants(status);
CREATE INDEX idx_tenants_industry ON tenants(industry);
CREATE INDEX idx_tenants_subscription ON tenants(subscription_tier);
CREATE INDEX idx_tenants_active ON tenants(created_at) WHERE deleted_at IS NULL;

ALTER TABLE tenants ADD CONSTRAINT uk_tenants_tenant_code UNIQUE (tenant_code);

CREATE TRIGGER trg_tenants_tenant_code BEFORE INSERT ON tenants
FOR EACH ROW EXECUTE FUNCTION generate_tenant_code();

-- ============================================================================
-- tenant_settings
-- ============================================================================
CREATE TABLE tenant_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    setting_key VARCHAR(100) NOT NULL,
    setting_value TEXT,
    setting_type VARCHAR(50) NOT NULL DEFAULT 'STRING',
    category VARCHAR(50),
    is_encrypted BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_tenant_settings_tenant ON tenant_settings(tenant_id, setting_key);
CREATE INDEX idx_tenant_settings_category ON tenant_settings(category);

ALTER TABLE tenant_settings ADD CONSTRAINT uk_tenant_settings_tenant_key UNIQUE (tenant_id, setting_key);

-- ============================================================================
-- tenant_features
-- ============================================================================
CREATE TABLE tenant_features (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    feature_key VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT false,
    config JSONB DEFAULT '{}'::jsonb,
    updated_by UUID,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_tenant_features_tenant ON tenant_features(tenant_id, feature_key);

ALTER TABLE tenant_features ADD CONSTRAINT uk_tenant_features_tenant_key UNIQUE (tenant_id, feature_key);

-- ============================================================================
-- tenant_limits
-- ============================================================================
CREATE TABLE tenant_limits (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    limit_key VARCHAR(100) NOT NULL,
    limit_value BIGINT NOT NULL,
    current_usage BIGINT NOT NULL DEFAULT 0,
    reset_period VARCHAR(20) NOT NULL DEFAULT 'MONTHLY',
    last_reset_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_tenant_limits_tenant ON tenant_limits(tenant_id, limit_key);

ALTER TABLE tenant_limits ADD CONSTRAINT uk_tenant_limits_tenant_key UNIQUE (tenant_id, limit_key);

-- ============================================================================
-- tenant_usage
-- ============================================================================
CREATE TABLE tenant_usage (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    usage_type VARCHAR(50) NOT NULL,
    count BIGINT NOT NULL DEFAULT 0,
    date DATE NOT NULL DEFAULT CURRENT_DATE,
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenant_usage_tenant ON tenant_usage(tenant_id, date DESC);
CREATE INDEX idx_tenant_usage_type ON tenant_usage(usage_type);
CREATE INDEX idx_tenant_usage_date ON tenant_usage(date);

-- ============================================================================
-- tenant_members
-- ============================================================================
CREATE TABLE tenant_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(50) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    left_at TIMESTAMPTZ,
    invited_by UUID,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_tenant_members_tenant ON tenant_members(tenant_id);
CREATE INDEX idx_tenant_members_user ON tenant_members(user_id);

ALTER TABLE tenant_members ADD CONSTRAINT uk_tenant_members_tenant_user UNIQUE (tenant_id, user_id);

-- ============================================================================
-- tenant_invitations
-- ============================================================================
CREATE TABLE tenant_invitations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    email VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,
    token VARCHAR(255) NOT NULL,
    invited_by UUID NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    accepted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_tenant_invitations_tenant ON tenant_invitations(tenant_id);
CREATE INDEX idx_tenant_invitations_email ON tenant_invitations(email);
CREATE INDEX idx_tenant_invitations_token ON tenant_invitations(token);
CREATE INDEX idx_tenant_invitations_expires ON tenant_invitations(expires_at);

-- ============================================================================
-- tenant_backup_schedule
-- ============================================================================
CREATE TABLE tenant_backup_schedule (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    frequency VARCHAR(20) NOT NULL DEFAULT 'DAILY',
    retention_days INTEGER NOT NULL DEFAULT 30,
    enabled BOOLEAN NOT NULL DEFAULT true,
    last_backup_at TIMESTAMPTZ,
    next_backup_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_tenant_backup_tenant ON tenant_backup_schedule(tenant_id);

ALTER TABLE tenant_backup_schedule ADD CONSTRAINT uk_tenant_backup_tenant UNIQUE (tenant_id);

-- ============================================================================
-- tenant_retention_policy
-- ============================================================================
CREATE TABLE tenant_retention_policy (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    data_type VARCHAR(50) NOT NULL,
    retention_days INTEGER NOT NULL,
    archive_after_days INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_tenant_retention_tenant ON tenant_retention_policy(tenant_id);

ALTER TABLE tenant_retention_policy ADD CONSTRAINT uk_tenant_retention_tenant_type UNIQUE (tenant_id, data_type);

-- ============================================================================
-- tenant_audit_retention
-- ============================================================================
CREATE TABLE tenant_audit_retention (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    audit_type VARCHAR(50) NOT NULL,
    retention_days INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_tenant_audit_retention_tenant ON tenant_audit_retention(tenant_id);

ALTER TABLE tenant_audit_retention ADD CONSTRAINT uk_tenant_audit_retention_tenant_type UNIQUE (tenant_id, audit_type);

-- ============================================================================
-- Foreign Key Constraints
-- ============================================================================
ALTER TABLE tenant_settings ADD CONSTRAINT fk_tenant_settings_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE tenant_features ADD CONSTRAINT fk_tenant_features_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE tenant_limits ADD CONSTRAINT fk_tenant_limits_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE tenant_usage ADD CONSTRAINT fk_tenant_usage_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE tenant_members ADD CONSTRAINT fk_tenant_members_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE tenant_members ADD CONSTRAINT fk_tenant_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE;
ALTER TABLE tenant_invitations ADD CONSTRAINT fk_tenant_invitations_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE tenant_backup_schedule ADD CONSTRAINT fk_tenant_backup_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE tenant_retention_policy ADD CONSTRAINT fk_tenant_retention_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE tenant_audit_retention ADD CONSTRAINT fk_tenant_audit_retention_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;


-- Deferred tenant FKs from V002 (tenants must exist first)
ALTER TABLE users ADD CONSTRAINT fk_users_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE roles ADD CONSTRAINT fk_roles_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE api_keys ADD CONSTRAINT fk_api_keys_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
-- Add triggers
CREATE TRIGGER trg_tenants_updated_at BEFORE UPDATE ON tenants
FOR EACH ROW EXECUTE FUNCTION update_updated_at();
