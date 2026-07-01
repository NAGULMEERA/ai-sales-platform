-- ============================================================================
-- V007__appointment.sql
-- Appointment: Appointments, Reminders, Calendar Sync
-- ============================================================================

-- ============================================================================
-- appointments
-- ============================================================================
CREATE TABLE appointments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    lead_id UUID NOT NULL,
    sales_user_id UUID NOT NULL,
    start_time TIMESTAMPTZ NOT NULL,
    end_time TIMESTAMPTZ NOT NULL,
    status appointment_status NOT NULL DEFAULT 'SCHEDULED',
    calendar_event_id VARCHAR(255),
    meeting_link TEXT,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    deleted_at TIMESTAMPTZ NULL,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_appointments_tenant_start ON appointments(tenant_id, start_time);
CREATE INDEX idx_appointments_lead ON appointments(lead_id);
CREATE INDEX idx_appointments_user ON appointments(sales_user_id);
CREATE INDEX idx_appointments_status ON appointments(status);
CREATE INDEX idx_appointments_upcoming ON appointments(tenant_id, start_time) WHERE status IN ('SCHEDULED', 'CONFIRMED');
CREATE INDEX idx_appointments_active ON appointments(tenant_id, start_time) WHERE deleted_at IS NULL;

-- ============================================================================
-- appointment_reminders
-- ============================================================================
CREATE TABLE appointment_reminders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL,
    reminder_type VARCHAR(20) NOT NULL,
    scheduled_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    channel VARCHAR(50) NOT NULL,
    status notification_status NOT NULL DEFAULT 'PENDING',
    retry_count INTEGER DEFAULT 0,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_appointment_reminders_appointment ON appointment_reminders(appointment_id);
CREATE INDEX idx_appointment_reminders_scheduled ON appointment_reminders(scheduled_at);
CREATE INDEX idx_appointment_reminders_status ON appointment_reminders(status);

-- ============================================================================
-- appointment_status_history
-- ============================================================================
CREATE TABLE appointment_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    appointment_id UUID NOT NULL,
    old_status appointment_status NOT NULL,
    new_status appointment_status NOT NULL,
    changed_by UUID,
    reason TEXT,
    changed_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_appointment_status_appointment ON appointment_status_history(appointment_id, changed_at DESC);

-- ============================================================================
-- calendar_sync_logs
-- ============================================================================
CREATE TABLE calendar_sync_logs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    sync_type VARCHAR(20) NOT NULL,
    sync_status VARCHAR(20) NOT NULL,
    appointment_id UUID,
    calendar_event_id VARCHAR(255),
    error_message TEXT,
    started_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    completed_at TIMESTAMPTZ,
    metadata JSONB DEFAULT '{}'::jsonb
);

CREATE INDEX idx_calendar_sync_user ON calendar_sync_logs(user_id, started_at DESC);
CREATE INDEX idx_calendar_sync_appointment ON calendar_sync_logs(appointment_id);

-- ============================================================================
-- Foreign Key Constraints
-- ============================================================================
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE;
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_lead FOREIGN KEY (lead_id) REFERENCES leads(id) ON DELETE CASCADE;
ALTER TABLE appointments ADD CONSTRAINT fk_appointments_user FOREIGN KEY (sales_user_id) REFERENCES users(id);
ALTER TABLE appointment_reminders ADD CONSTRAINT fk_appointment_reminders_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE;
ALTER TABLE appointment_status_history ADD CONSTRAINT fk_appointment_status_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE;
ALTER TABLE calendar_sync_logs ADD CONSTRAINT fk_calendar_sync_user FOREIGN KEY (user_id) REFERENCES users(id);

-- Add triggers
CREATE TRIGGER trg_appointments_updated_at BEFORE UPDATE ON appointments
FOR EACH ROW EXECUTE FUNCTION update_updated_at();
