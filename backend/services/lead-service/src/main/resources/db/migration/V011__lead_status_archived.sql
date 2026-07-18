-- Terminal archive stage for closed leads (immutable history retained).
ALTER TYPE lead_status ADD VALUE IF NOT EXISTS 'ARCHIVED';
