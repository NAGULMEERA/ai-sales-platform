-- Lead Service: foundation subset (production)
CREATE EXTENSION IF NOT EXISTS pgcrypto;
CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE TYPE lead_status AS ENUM (
    'NEW', 'CONTACTED', 'QUALIFIED', 'APPOINTMENT_BOOKED',
    'VISITED', 'NEGOTIATING', 'WON', 'LOST'
);

CREATE OR REPLACE FUNCTION update_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION generate_lead_external_id()
RETURNS TRIGGER AS $$
BEGIN
    NEW.external_id = 'LD-' || TO_CHAR(NOW(), 'YYYYMMDD') || '-' ||
        LPAD(CAST(nextval('lead_external_seq') AS TEXT), 8, '0');
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE SEQUENCE IF NOT EXISTS lead_external_seq START 100000;
