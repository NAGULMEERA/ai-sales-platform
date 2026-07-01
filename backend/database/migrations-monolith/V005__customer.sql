-- ============================================================================
-- V005__customer.sql
-- Customer: Customers, Profiles, Journey, Interactions
-- ============================================================================

-- ============================================================================
-- customers
-- ============================================================================
CREATE TABLE customers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    external_id VARCHAR(255),
    first_name VARCHAR(255) NOT NULL,
    last_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    unified_phone VARCHAR(50),
    unified_email VARCHAR(255),
    lifecycle_stage VARCHAR(50) NOT NULL DEFAULT 'LEAD',
    metadata JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ NULL
);

CREATE INDEX idx_customers_tenant_phone ON customers(tenant_id, phone);
CREATE INDEX idx_customers_tenant_email ON customers(tenant_id, email);
CREATE INDEX idx_customers_unified_phone ON customers(unified_phone);
CREATE INDEX idx_customers_unified_email ON customers(unified_email);
CREATE INDEX idx_customers_lifecycle ON customers(lifecycle_stage);
CREATE INDEX idx_customers_active ON customers(tenant_id, created_at) WHERE deleted_at IS NULL;

-- ============================================================================
-- customer_profiles
-- ============================================================================
CREATE TABLE customer_profiles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    profile_type VARCHAR(50) NOT NULL DEFAULT 'DEFAULT',
    data JSONB DEFAULT '{}'::jsonb,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_customer_profiles_customer ON customer_profiles(customer_id);

-- ============================================================================
-- customer_addresses
-- ============================================================================
CREATE TABLE customer_addresses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    address_type VARCHAR(50) NOT NULL,
    line1 VARCHAR(255) NOT NULL,
    line2 VARCHAR(255),
    city VARCHAR(100) NOT NULL,
    state VARCHAR(100) NOT NULL,
    pincode VARCHAR(20) NOT NULL,
    country VARCHAR(100) NOT NULL DEFAULT 'India',
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_customer_addresses_customer ON customer_addresses(customer_id);

-- ============================================================================
-- customer_documents
-- ============================================================================
CREATE TABLE customer_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    document_url TEXT NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    verified BOOLEAN DEFAULT false,
    verified_at TIMESTAMPTZ,
    verified_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_customer_documents_customer ON customer_documents(customer_id);

-- ============================================================================
-- customer_preferences
-- ============================================================================
CREATE TABLE customer_preferences (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    preference_key VARCHAR(100) NOT NULL,
    preference_value TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_preferences_customer ON customer_preferences(customer_id, preference_key);

-- ============================================================================
-- customer_relationships
-- ============================================================================
CREATE TABLE customer_relationships (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    related_customer_id UUID NOT NULL,
    relationship_type VARCHAR(50) NOT NULL,
    relationship_data JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_customer_relationships_customer ON customer_relationships(customer_id);
CREATE INDEX idx_customer_relationships_related ON customer_relationships(related_customer_id);

-- ============================================================================
-- customer_segments
-- ============================================================================
CREATE TABLE customer_segments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    segment_name VARCHAR(255) NOT NULL,
    segment_description TEXT,
    segment_criteria JSONB NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_customer_segments_tenant ON customer_segments(tenant_id);

-- ============================================================================
-- customer_segment_members
-- ============================================================================
CREATE TABLE customer_segment_members (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    segment_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    added_by UUID
);

CREATE INDEX idx_customer_segment_members_segment ON customer_segment_members(segment_id);
CREATE INDEX idx_customer_segment_members_customer ON customer_segment_members(customer_id);

ALTER TABLE customer_segment_members ADD CONSTRAINT uk_customer_segment_member UNIQUE (segment_id, customer_id);

-- ============================================================================
-- customer_timeline
-- ============================================================================
CREATE TABLE customer_timeline (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    event_type VARCHAR(50) NOT NULL,
    event_data JSONB DEFAULT '{}'::jsonb,
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    source VARCHAR(50),
    created_by UUID,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_customer_timeline_customer ON customer_timeline(customer_id, occurred_at DESC);

-- ============================================================================
-- customer_interactions
-- ============================================================================
CREATE TABLE customer_interactions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    interaction_type VARCHAR(50) NOT NULL,
    channel VARCHAR(50) NOT NULL,
    details JSONB DEFAULT '{}'::jsonb,
    interaction_data JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_customer_interactions_customer ON customer_interactions(customer_id, created_at DESC);

-- ============================================================================
-- customer_consents
-- ============================================================================
CREATE TABLE customer_consents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_id UUID NOT NULL,
    consent_type VARCHAR(100) NOT NULL,
    consent_version VARCHAR(20) NOT NULL,
    granted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    withdrawn_at TIMESTAMPTZ,
    ip_address INET,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_customer_consents_customer ON customer_consents(customer_id);

-- ============================================================================
-- customer_tags
-- ============================================================================
CREATE TABLE customer_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    tag VARCHAR(100) NOT NULL,
    added_by UUID,
    added_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_customer_tags_customer ON customer_tags(customer_id);
CREATE INDEX idx_customer_tags_tenant ON customer_tags(tenant_id);

ALTER TABLE customer_tags ADD CONSTRAINT uk_customer_tags_customer_tag UNIQUE (customer_id, tag);

-- ============================================================================
-- Foreign Key Constraints
-- ============================================================================
ALTER TABLE customers ADD CONSTRAINT fk_customers_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE customer_profiles ADD CONSTRAINT fk_customer_profiles_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
ALTER TABLE customer_addresses ADD CONSTRAINT fk_customer_addresses_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
ALTER TABLE customer_documents ADD CONSTRAINT fk_customer_documents_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
ALTER TABLE customer_preferences ADD CONSTRAINT fk_customer_preferences_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
ALTER TABLE customer_relationships ADD CONSTRAINT fk_customer_relationships_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
ALTER TABLE customer_relationships ADD CONSTRAINT fk_customer_relationships_related FOREIGN KEY (related_customer_id) REFERENCES customers(id) ON DELETE CASCADE;
ALTER TABLE customer_segment_members ADD CONSTRAINT fk_customer_segment_members_segment FOREIGN KEY (segment_id) REFERENCES customer_segments(id) ON DELETE CASCADE;
ALTER TABLE customer_segment_members ADD CONSTRAINT fk_customer_segment_members_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
ALTER TABLE customer_timeline ADD CONSTRAINT fk_customer_timeline_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
ALTER TABLE customer_interactions ADD CONSTRAINT fk_customer_interactions_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
ALTER TABLE customer_consents ADD CONSTRAINT fk_customer_consents_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;
ALTER TABLE customer_tags ADD CONSTRAINT fk_customer_tags_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE CASCADE;


-- Deferred customer FK from V004
ALTER TABLE leads ADD CONSTRAINT fk_leads_customer FOREIGN KEY (customer_id) REFERENCES customers(id) ON DELETE SET NULL;
-- Add triggers
CREATE TRIGGER trg_customers_updated_at BEFORE UPDATE ON customers
FOR EACH ROW EXECUTE FUNCTION update_updated_at();
