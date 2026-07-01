-- ============================================================================
-- V026__views.sql
-- Operational views (DB.html v7 + DDS-compatible filters)
-- ============================================================================

CREATE OR REPLACE VIEW v_lead_funnel AS
SELECT tenant_id, status, COUNT(*) AS lead_count, DATE_TRUNC('day', created_at) AS day
FROM leads
WHERE status NOT IN ('WON', 'LOST')
GROUP BY tenant_id, status, DATE_TRUNC('day', created_at);

CREATE OR REPLACE VIEW v_tenant_active_users AS
SELECT tenant_id, COUNT(*) AS active_users
FROM users
WHERE status = 'ACTIVE'
GROUP BY tenant_id;

CREATE OR REPLACE VIEW v_pending_notifications AS
SELECT tenant_id, channel, COUNT(*) AS pending_count
FROM notifications
WHERE status = 'PENDING'
GROUP BY tenant_id, channel;
