-- ============================================================================
-- AI Sales Platform
-- Flyway Migration : V003__schemas.sql
-- Purpose          : Create enterprise database schemas
-- Depends On       : V001__baseline.sql, V002__extensions.sql
-- PostgreSQL       : 17+
-- ============================================================================

BEGIN;

-- ----------------------------------------------------------------------------
-- Schemas
-- ----------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS core;
CREATE SCHEMA IF NOT EXISTS tenant;
CREATE SCHEMA IF NOT EXISTS identity;
CREATE SCHEMA IF NOT EXISTS crm;
CREATE SCHEMA IF NOT EXISTS catalog;
CREATE SCHEMA IF NOT EXISTS conversation;
CREATE SCHEMA IF NOT EXISTS workflow;
CREATE SCHEMA IF NOT EXISTS appointment;
CREATE SCHEMA IF NOT EXISTS notification;
CREATE SCHEMA IF NOT EXISTS analytics;
CREATE SCHEMA IF NOT EXISTS ai;

-- ----------------------------------------------------------------------------
-- Schema Comments
-- ----------------------------------------------------------------------------
COMMENT ON SCHEMA core IS 'Shared platform objects';
COMMENT ON SCHEMA tenant IS 'Tenant management';
COMMENT ON SCHEMA identity IS 'Authentication and authorization';
COMMENT ON SCHEMA crm IS 'CRM domain';
COMMENT ON SCHEMA catalog IS 'Catalog domain';
COMMENT ON SCHEMA conversation IS 'Conversation domain';
COMMENT ON SCHEMA workflow IS 'Workflow domain';
COMMENT ON SCHEMA appointment IS 'Appointment domain';
COMMENT ON SCHEMA notification IS 'Notification domain';
COMMENT ON SCHEMA analytics IS 'Analytics domain';
COMMENT ON SCHEMA ai IS 'AI platform';

-- ----------------------------------------------------------------------------
-- Default Permissions
-- NOTE: Adjust roles to match your deployment environment.
-- ----------------------------------------------------------------------------
REVOKE ALL ON SCHEMA core FROM PUBLIC;
REVOKE ALL ON SCHEMA tenant FROM PUBLIC;
REVOKE ALL ON SCHEMA identity FROM PUBLIC;
REVOKE ALL ON SCHEMA crm FROM PUBLIC;
REVOKE ALL ON SCHEMA catalog FROM PUBLIC;
REVOKE ALL ON SCHEMA conversation FROM PUBLIC;
REVOKE ALL ON SCHEMA workflow FROM PUBLIC;
REVOKE ALL ON SCHEMA appointment FROM PUBLIC;
REVOKE ALL ON SCHEMA notification FROM PUBLIC;
REVOKE ALL ON SCHEMA analytics FROM PUBLIC;
REVOKE ALL ON SCHEMA ai FROM PUBLIC;

COMMIT;
