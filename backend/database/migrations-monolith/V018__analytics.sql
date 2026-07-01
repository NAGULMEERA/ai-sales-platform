-- ============================================================================
-- V018__analytics.sql
-- Generated from DB.html v7.0: Analytics and deals (§18)
-- Lab monolith only. Split per service for production (UUID refs, no cross-FK).
-- ============================================================================

-- DB.html 18.1 analytics_kpis
CREATE TABLE analytics_kpis (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    kpi_name TEXT,
    value DECIMAL(19,4),
    period_start DATE,
    period_end DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 18.2 conversion_funnels
CREATE TABLE conversion_funnels (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    stage TEXT,
    count INTEGER,
    conversion_rate DECIMAL(19,4),
    period_start DATE,
    period_end DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 18.3 attribution_reports
CREATE TABLE attribution_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    channel TEXT,
    leads INTEGER,
    appointments INTEGER,
    revenue DECIMAL(19,4),
    cost DECIMAL(19,4),
    roi DECIMAL(19,4),
    period_start DATE,
    period_end DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 18.4 tenant_dashboards
CREATE TABLE tenant_dashboards (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    dashboard_data JSONB DEFAULT '{}'::jsonb,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 18.5 opportunities
CREATE TABLE opportunities (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    customer_id UUID,
    lead_id UUID,
    name TEXT,
    value DECIMAL(19,4),
    stage TEXT,
    probability INTEGER,
    expected_close_date DATE,
    assigned_to UUID,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_opportunities_tenant ON opportunities(tenant_id);
CREATE INDEX IF NOT EXISTS idx_opportunities_customer ON opportunities(customer_id);
CREATE INDEX IF NOT EXISTS idx_opportunities_stage ON opportunities(stage);

-- DB.html 18.6 sales_outcomes
CREATE TABLE sales_outcomes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    opportunity_id UUID,
    outcome_type TEXT,
    revenue DECIMAL(19,4),
    reason TEXT,
    closed_by UUID,
    closed_at TIMESTAMPTZ
);


-- DB.html 18.7 revenue_tracking
CREATE TABLE revenue_tracking (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    customer_id UUID,
    opportunity_id UUID,
    source TEXT,
    amount DECIMAL(19,4),
    booking_date DATE,
    collection_date DATE,
    status TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

