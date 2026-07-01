-- ============================================================================
-- V001__foundation.sql
-- Foundation: Extensions, ENUMs, Standard Columns
-- ============================================================================

-- Enable Required Extensions
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;
CREATE EXTENSION IF NOT EXISTS vector;
CREATE EXTENSION IF NOT EXISTS btree_gin;
CREATE EXTENSION IF NOT EXISTS btree_gist;

-- ============================================================================
-- ENUM Types
-- ============================================================================

CREATE TYPE tenant_status AS ENUM ('TRIAL', 'ACTIVE', 'SUSPENDED', 'EXPIRED');
CREATE TYPE user_status AS ENUM ('ACTIVE', 'INACTIVE', 'LOCKED', 'PENDING_VERIFICATION');
CREATE TYPE lead_status AS ENUM ('NEW', 'CONTACTED', 'QUALIFIED', 'APPOINTMENT_BOOKED', 'VISITED', 'NEGOTIATING', 'WON', 'LOST');
CREATE TYPE appointment_status AS ENUM ('SCHEDULED', 'CONFIRMED', 'COMPLETED', 'CANCELLED', 'NO_SHOW');
CREATE TYPE workflow_status AS ENUM ('DRAFT', 'PUBLISHED', 'ARCHIVED', 'DEPRECATED');
CREATE TYPE notification_status AS ENUM ('PENDING', 'SENT', 'DELIVERED', 'READ', 'FAILED', 'BOUNCED');

-- ============================================================================
-- Common Functions
-- ============================================================================

-- Update timestamp trigger function
CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Generate tenant code function
CREATE OR REPLACE FUNCTION generate_tenant_code()
RETURNS TRIGGER AS $$
BEGIN
    NEW.tenant_code = 'TEN' || LPAD(CAST(nextval('tenant_code_seq') AS TEXT), 6, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Create tenant_code sequence
CREATE SEQUENCE IF NOT EXISTS tenant_code_seq START 100000;

-- Generate invoice number function
CREATE OR REPLACE FUNCTION generate_invoice_number()
RETURNS TRIGGER AS $$
BEGIN
    NEW.invoice_number = 'INV-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-' || LPAD(CAST(nextval('invoice_seq') AS TEXT), 6, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE SEQUENCE IF NOT EXISTS invoice_seq START 100000;

-- Generate lead external ID function
CREATE OR REPLACE FUNCTION generate_lead_external_id()
RETURNS TRIGGER AS $$
BEGIN
    NEW.external_id = 'LD-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-' || LPAD(CAST(nextval('lead_external_seq') AS TEXT), 8, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE SEQUENCE IF NOT EXISTS lead_external_seq START 100000;

-- JSONB merge function
CREATE OR REPLACE FUNCTION jsonb_merge(current JSONB, updates JSONB)
RETURNS JSONB AS $$
BEGIN
    RETURN COALESCE(current, '{}'::jsonb) || COALESCE(updates, '{}'::jsonb);
END;
$$ LANGUAGE plpgsql IMMUTABLE;
