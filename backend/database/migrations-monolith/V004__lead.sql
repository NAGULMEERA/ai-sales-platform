-- ============================================================================
-- V004__lead.sql
-- Lead: Leads, Assignments, Scoring, Attribution
-- ============================================================================

-- ============================================================================
-- leads
-- ============================================================================
CREATE TABLE leads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID,
    external_id VARCHAR(255),
    source_type VARCHAR(50) NOT NULL,
    source_id VARCHAR(255),
    campaign VARCHAR(255),
    adset VARCHAR(255),
    ad VARCHAR(255),
    utm_source VARCHAR(255),
    utm_campaign VARCHAR(255),
    customer_name VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NOT NULL,
    email VARCHAR(255),
    custom_fields JSONB DEFAULT '{}'::jsonb,
    qualified BOOLEAN DEFAULT false,
    score INTEGER,
    confidence_score INTEGER,
    status lead_status NOT NULL DEFAULT 'NEW',
    assigned_to UUID,
    transcript TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ NULL,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_leads_tenant_status ON leads(tenant_id, status);
CREATE INDEX idx_leads_tenant_created ON leads(tenant_id, created_at DESC);
CREATE INDEX idx_leads_tenant_assigned ON leads(tenant_id, assigned_to);
CREATE INDEX idx_leads_phone ON leads(phone);
CREATE INDEX idx_leads_email ON leads(email);
CREATE INDEX idx_leads_external_id ON leads(external_id);
CREATE INDEX idx_leads_score ON leads(score) WHERE score > 70;
CREATE INDEX idx_leads_active ON leads(tenant_id, created_at) WHERE deleted_at IS NULL;

ALTER TABLE leads ADD CONSTRAINT ck_leads_score CHECK (score BETWEEN 0 AND 100);
ALTER TABLE leads ADD CONSTRAINT ck_leads_confidence CHECK (confidence_score BETWEEN 0 AND 100);

-- ============================================================================
-- lead_sources
-- ============================================================================
CREATE TABLE lead_sources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    source_type VARCHAR(50) NOT NULL,
    source_name VARCHAR(255),
    config JSONB DEFAULT '{}'::jsonb,
    enabled BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_lead_sources_tenant ON lead_sources(tenant_id);
CREATE INDEX idx_lead_sources_type ON lead_sources(source_type);

-- ============================================================================
-- lead_assignments
-- ============================================================================
CREATE TABLE lead_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL,
    assigned_to UUID NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    unassigned_at TIMESTAMPTZ,
    assignment_reason VARCHAR(255),
    created_by UUID,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_lead_assignments_lead ON lead_assignments(lead_id);
CREATE INDEX idx_lead_assignments_user ON lead_assignments(assigned_to);

-- ============================================================================
-- lead_scores
-- ============================================================================
CREATE TABLE lead_scores (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL,
    score INTEGER NOT NULL,
    score_type VARCHAR(50) NOT NULL,
    factors JSONB DEFAULT '{}'::jsonb,
    explanation TEXT,
    scored_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    scored_by UUID,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_lead_scores_lead ON lead_scores(lead_id);
CREATE INDEX idx_lead_scores_type ON lead_scores(score_type);

ALTER TABLE lead_scores ADD CONSTRAINT ck_lead_scores_score CHECK (score BETWEEN 0 AND 100);

-- ============================================================================
-- lead_status_history
-- ============================================================================
CREATE TABLE lead_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL,
    old_status lead_status NOT NULL,
    new_status lead_status NOT NULL,
    reason TEXT,
    changed_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_lead_status_lead ON lead_status_history(lead_id);
CREATE INDEX idx_lead_status_created ON lead_status_history(created_at DESC);

-- ============================================================================
-- lead_activities
-- ============================================================================
CREATE TABLE lead_activities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    description TEXT,
    activity_data JSONB DEFAULT '{}'::jsonb,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_lead_activities_lead ON lead_activities(lead_id, created_at DESC);

-- ============================================================================
-- lead_notes
-- ============================================================================
CREATE TABLE lead_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL,
    note TEXT NOT NULL,
    note_type VARCHAR(50) DEFAULT 'GENERAL',
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_lead_notes_lead ON lead_notes(lead_id, created_at DESC);

-- ============================================================================
-- lead_tags
-- ============================================================================
CREATE TABLE lead_tags (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    tag VARCHAR(100) NOT NULL,
    color VARCHAR(20),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_lead_tags_tenant ON lead_tags(tenant_id);
CREATE INDEX idx_lead_tags_tag ON lead_tags(tag);

ALTER TABLE lead_tags ADD CONSTRAINT uk_lead_tags_tenant_tag UNIQUE (tenant_id, tag);

-- ============================================================================
-- lead_tag_mapping
-- ============================================================================
CREATE TABLE lead_tag_mapping (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL,
    tag_id UUID NOT NULL,
    assigned_by UUID,
    assigned_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lead_tag_mapping_lead ON lead_tag_mapping(lead_id);
CREATE INDEX idx_lead_tag_mapping_tag ON lead_tag_mapping(tag_id);

ALTER TABLE lead_tag_mapping ADD CONSTRAINT uk_lead_tag_mapping_lead_tag UNIQUE (lead_id, tag_id);

-- ============================================================================
-- lead_attachments
-- ============================================================================
CREATE TABLE lead_attachments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL,
    file_name VARCHAR(255) NOT NULL,
    file_url TEXT NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,
    uploaded_by UUID,
    uploaded_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_lead_attachments_lead ON lead_attachments(lead_id);

-- ============================================================================
-- lead_custom_fields
-- ============================================================================
CREATE TABLE lead_custom_fields (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    field_name VARCHAR(100) NOT NULL,
    field_type VARCHAR(50) NOT NULL,
    field_options JSONB DEFAULT '{}'::jsonb,
    is_required BOOLEAN DEFAULT false,
    display_order INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_lead_custom_fields_tenant ON lead_custom_fields(tenant_id);

-- ============================================================================
-- lead_followups
-- ============================================================================
CREATE TABLE lead_followups (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    followup_type VARCHAR(50) NOT NULL,
    note TEXT,
    assigned_to UUID,
    created_by UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_lead_followups_lead ON lead_followups(lead_id);
CREATE INDEX idx_lead_followups_scheduled ON lead_followups(scheduled_at);

-- ============================================================================
-- lead_duplicates
-- ============================================================================
CREATE TABLE lead_duplicates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    lead_id UUID NOT NULL,
    duplicate_of_lead_id UUID NOT NULL,
    similarity_score DECIMAL(3,2) NOT NULL,
    detected_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    resolved BOOLEAN DEFAULT false,
    merged_into_lead_id UUID,
    merged_at TIMESTAMPTZ,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_lead_duplicates_tenant ON lead_duplicates(tenant_id);
CREATE INDEX idx_lead_duplicates_lead ON lead_duplicates(lead_id);
CREATE INDEX idx_lead_duplicates_master ON lead_duplicates(duplicate_of_lead_id);
CREATE INDEX idx_lead_duplicates_resolved ON lead_duplicates(resolved) WHERE resolved = false;

ALTER TABLE lead_duplicates ADD CONSTRAINT ck_lead_duplicates_score CHECK (similarity_score BETWEEN 0 AND 1);

-- ============================================================================
-- lead_attribution
-- ============================================================================
CREATE TABLE lead_attribution (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL,
    channel VARCHAR(50) NOT NULL,
    campaign VARCHAR(255),
    ad_id VARCHAR(255),
    position INTEGER,
    cost DECIMAL(19,4),
    source_details JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_lead_attribution_lead ON lead_attribution(lead_id);
CREATE INDEX idx_lead_attribution_channel ON lead_attribution(channel);

-- ============================================================================
-- lead_quality_score
-- ============================================================================
CREATE TABLE lead_quality_score (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    lead_id UUID NOT NULL,
    overall_score INTEGER NOT NULL,
    budget_fit VARCHAR(20),
    timeline VARCHAR(20),
    decision_maker VARCHAR(20),
    competitor_awareness VARCHAR(20),
    objections TEXT[],
    suggested_product TEXT,
    next_action TEXT,
    raw_llm_response JSONB DEFAULT '{}'::jsonb,
    scored_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_lead_quality_score_lead ON lead_quality_score(lead_id);
CREATE INDEX idx_lead_quality_score_score ON lead_quality_score(overall_score);

ALTER TABLE lead_quality_score ADD CONSTRAINT ck_lead_quality_score CHECK (overall_score BETWEEN 0 AND 100);

-- ============================================================================
-- Foreign Key Constraints
-- Note: fk_leads_customer deferred to V005 (customers table created there)
-- ============================================================================
ALTER TABLE leads ADD CONSTRAINT fk_leads_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE lead_assignments ADD CONSTRAINT fk_lead_assignments_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;
ALTER TABLE lead_assignments ADD CONSTRAINT fk_lead_assignments_user FOREIGN KEY (assigned_to) REFERENCES users(id);
ALTER TABLE lead_scores ADD CONSTRAINT fk_lead_scores_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;
ALTER TABLE lead_status_history ADD CONSTRAINT fk_lead_status_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;
ALTER TABLE lead_activities ADD CONSTRAINT fk_lead_activities_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;
ALTER TABLE lead_notes ADD CONSTRAINT fk_lead_notes_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;
ALTER TABLE lead_tag_mapping ADD CONSTRAINT fk_lead_tag_mapping_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;
ALTER TABLE lead_tag_mapping ADD CONSTRAINT fk_lead_tag_mapping_tag FOREIGN KEY (tag_id) REFERENCES lead_tags(id) ON DELETE CASCADE;
ALTER TABLE lead_attachments ADD CONSTRAINT fk_lead_attachments_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;
ALTER TABLE lead_followups ADD CONSTRAINT fk_lead_followups_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;
ALTER TABLE lead_duplicates ADD CONSTRAINT fk_lead_duplicates_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;
ALTER TABLE lead_duplicates ADD CONSTRAINT fk_lead_duplicates_master FOREIGN KEY (duplicate_of_lead_id) REFERENCES leads(id) ON DELETE CASCADE;
ALTER TABLE lead_attribution ADD CONSTRAINT fk_lead_attribution_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;
ALTER TABLE lead_quality_score ADD CONSTRAINT fk_lead_quality_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;

-- Add triggers
CREATE TRIGGER trg_leads_updated_at BEFORE UPDATE ON leads
FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_leads_external_id BEFORE INSERT ON leads
FOR EACH ROW EXECUTE FUNCTION generate_lead_external_id();
