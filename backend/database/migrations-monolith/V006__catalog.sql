-- ============================================================================
-- V006__catalog.sql
-- Catalog: Builders, Projects, Properties, Images, Embeddings
-- ============================================================================

-- ============================================================================
-- builders
-- ============================================================================
CREATE TABLE builders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    website VARCHAR(255),
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    rating DECIMAL(2,1),
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ NULL,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_builders_tenant ON builders(tenant_id);
CREATE INDEX idx_builders_active ON builders(tenant_id, is_active) WHERE deleted_at IS NULL;

-- ============================================================================
-- projects
-- ============================================================================
CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    builder_id UUID NOT NULL,
    code VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    project_type VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    start_date DATE,
    completion_date DATE,
    amenities JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ NULL,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_projects_tenant ON projects(tenant_id);
CREATE INDEX idx_projects_builder ON projects(builder_id);
CREATE INDEX idx_projects_code ON projects(code);
CREATE INDEX idx_projects_active ON projects(tenant_id, status) WHERE deleted_at IS NULL;

ALTER TABLE projects ADD CONSTRAINT uk_projects_code UNIQUE (code);

-- ============================================================================
-- properties
-- ============================================================================
CREATE TABLE properties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    project_id UUID NOT NULL,
    property_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    price DECIMAL(19,4) NOT NULL,
    bedrooms INTEGER,
    bathrooms INTEGER,
    sqft INTEGER,
    furnishing VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'ACTIVE',
    attributes JSONB DEFAULT '{}'::jsonb,
    location JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ NULL,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_properties_tenant ON properties(tenant_id);
CREATE INDEX idx_properties_project ON properties(project_id);
CREATE INDEX idx_properties_status ON properties(status);
CREATE INDEX idx_properties_price ON properties(price);
CREATE INDEX idx_properties_active ON properties(tenant_id, status) WHERE deleted_at IS NULL AND status = 'ACTIVE';

-- ============================================================================
-- property_units
-- ============================================================================
CREATE TABLE property_units (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL,
    unit_number VARCHAR(50) NOT NULL,
    floor_number INTEGER,
    area_sqft INTEGER,
    price DECIMAL(19,4),
    status VARCHAR(50) NOT NULL DEFAULT 'AVAILABLE',
    attributes JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_property_units_property ON property_units(property_id);
CREATE INDEX idx_property_units_status ON property_units(status);

-- ============================================================================
-- property_pricing
-- ============================================================================
CREATE TABLE property_pricing (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL,
    price DECIMAL(19,4) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_property_pricing_property ON property_pricing(property_id);

-- ============================================================================
-- property_images
-- ============================================================================
CREATE TABLE property_images (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL,
    image_url TEXT NOT NULL,
    thumbnail_url TEXT,
    caption VARCHAR(255),
    display_order INTEGER,
    is_primary BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_property_images_property ON property_images(property_id);
CREATE INDEX idx_property_images_primary ON property_images(property_id) WHERE is_primary = true;

-- ============================================================================
-- amenities
-- ============================================================================
CREATE TABLE amenities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    category VARCHAR(50),
    icon VARCHAR(50),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_amenities_tenant ON amenities(tenant_id);

-- ============================================================================
-- locations
-- ============================================================================
CREATE TABLE locations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    location_type VARCHAR(50) NOT NULL,
    name VARCHAR(255) NOT NULL,
    parent_id UUID,
    coordinates JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_locations_tenant ON locations(tenant_id);
CREATE INDEX idx_locations_parent ON locations(parent_id);

-- ============================================================================
-- inventory
-- ============================================================================
CREATE TABLE inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL,
    available_count INTEGER NOT NULL DEFAULT 0,
    total_count INTEGER NOT NULL DEFAULT 0,
    last_update_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_inventory_property ON inventory(property_id);

-- ============================================================================
-- catalog_categories
-- ============================================================================
CREATE TABLE catalog_categories (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    parent_id UUID,
    display_order INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_catalog_categories_tenant ON catalog_categories(tenant_id);
CREATE INDEX idx_catalog_categories_parent ON catalog_categories(parent_id);

-- ============================================================================
-- catalog_attributes
-- ============================================================================
CREATE TABLE catalog_attributes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    attribute_key VARCHAR(100) NOT NULL,
    attribute_type VARCHAR(50) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    is_filterable BOOLEAN DEFAULT false,
    options JSONB DEFAULT '[]'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_catalog_attributes_tenant ON catalog_attributes(tenant_id);

-- ============================================================================
-- catalog_embeddings
-- ============================================================================
CREATE TABLE catalog_embeddings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    property_id UUID NOT NULL,
    embedding VECTOR(1536),
    embedding_type VARCHAR(50) NOT NULL DEFAULT 'combined',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_catalog_embeddings_property ON catalog_embeddings(property_id);

-- ============================================================================
-- catalog_status_history
-- ============================================================================
CREATE TABLE catalog_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    catalog_item_id UUID NOT NULL,
    old_status VARCHAR(50) NOT NULL,
    new_status VARCHAR(50) NOT NULL,
    changed_by UUID,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    reason TEXT,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_catalog_status_item ON catalog_status_history(catalog_item_id, changed_at DESC);

-- ============================================================================
-- Foreign Key Constraints
-- ============================================================================
ALTER TABLE projects ADD CONSTRAINT fk_projects_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE projects ADD CONSTRAINT fk_projects_builder FOREIGN KEY (builder_id) REFERENCES builders(id);
ALTER TABLE properties ADD CONSTRAINT fk_properties_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE properties ADD CONSTRAINT fk_properties_project FOREIGN KEY (project_id) REFERENCES projects(id);
ALTER TABLE property_units ADD CONSTRAINT fk_property_units_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE;
ALTER TABLE property_pricing ADD CONSTRAINT fk_property_pricing_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE;
ALTER TABLE property_images ADD CONSTRAINT fk_property_images_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE;
ALTER TABLE inventory ADD CONSTRAINT fk_inventory_property FOREIGN KEY (property_id) REFERENCES properties(id);
ALTER TABLE catalog_categories ADD CONSTRAINT fk_catalog_categories_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE catalog_categories ADD CONSTRAINT fk_catalog_categories_parent FOREIGN KEY (parent_id) REFERENCES catalog_categories(id);
ALTER TABLE catalog_embeddings ADD CONSTRAINT fk_catalog_embeddings_property FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE;

-- Add triggers
CREATE TRIGGER trg_builders_updated_at BEFORE UPDATE ON builders
FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_projects_updated_at BEFORE UPDATE ON projects
FOR EACH ROW EXECUTE FUNCTION update_updated_at();

CREATE TRIGGER trg_properties_updated_at BEFORE UPDATE ON properties
FOR EACH ROW EXECUTE FUNCTION update_updated_at();
