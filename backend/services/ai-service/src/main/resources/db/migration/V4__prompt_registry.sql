-- Versioned prompt registry. Never overwrite applied versions — add new rows.

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TABLE prompt_template (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    organization_id UUID,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    purpose VARCHAR(100) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    active_version INT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT ck_prompt_template_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT uq_prompt_template_tenant_code UNIQUE (tenant_id, code)
);

CREATE INDEX idx_prompt_template_tenant_status ON prompt_template (tenant_id, status)
    WHERE deleted_at IS NULL;

CREATE TRIGGER trg_prompt_template_updated_at
    BEFORE UPDATE ON prompt_template
    FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TABLE prompt_version (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    prompt_id UUID NOT NULL REFERENCES prompt_template (id),
    version_number INT NOT NULL,
    system_template TEXT,
    user_template TEXT NOT NULL,
    variables JSONB NOT NULL DEFAULT '[]'::jsonb,
    expected_output_hint VARCHAR(2000),
    changelog VARCHAR(2000),
    status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    CONSTRAINT ck_prompt_version_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT uq_prompt_version UNIQUE (tenant_id, prompt_id, version_number)
);

CREATE INDEX idx_prompt_version_prompt ON prompt_version (tenant_id, prompt_id, version_number DESC);
