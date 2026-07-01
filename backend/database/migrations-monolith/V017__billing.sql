-- ============================================================================
-- V017__billing.sql
-- Generated from DB.html v7.0: Billing and cost protection (§8-9)
-- Lab monolith only. Split per service for production (UUID refs, no cross-FK).
-- ============================================================================

-- DB.html 8.1 plans
CREATE TABLE plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT,
    price_monthly DECIMAL(19,4),
    price_annual DECIMAL(19,4),
    features JSONB DEFAULT '{}'::jsonb,
    limits JSONB DEFAULT '{}'::jsonb,
    trial_days INTEGER,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 8.2 subscriptions
CREATE TABLE subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    plan_id UUID,
    status TEXT,
    trial_start TIMESTAMPTZ,
    trial_end TIMESTAMPTZ,
    current_period_start TIMESTAMPTZ,
    current_period_end TIMESTAMPTZ,
    cancel_at_period_end BOOLEAN DEFAULT false,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_subscriptions_tenant ON subscriptions(tenant_id);
CREATE INDEX IF NOT EXISTS idx_subscriptions_tenant_active ON subscriptions(tenant_id, status) WHERE status = 'ACTIVE';

-- DB.html 8.3 invoices
CREATE TABLE invoices (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    subscription_id UUID,
    invoice_number TEXT,
    amount DECIMAL(19,4),
    tax_amount DECIMAL(19,4),
    total_amount DECIMAL(19,4),
    status TEXT,
    due_date TIMESTAMPTZ,
    paid_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 8.4 payments
CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    invoice_id UUID,
    payment_method TEXT,
    transaction_id TEXT,
    amount DECIMAL(19,4),
    status TEXT,
    gateway_response JSONB DEFAULT '{}'::jsonb,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 8.5 usage_metering
CREATE TABLE usage_metering (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    channel TEXT,
    count INTEGER,
    date DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 8.6 usage_billing
CREATE TABLE usage_billing (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    usage_type TEXT,
    quantity DECIMAL(19,4),
    unit_price DECIMAL(19,4),
    total DECIMAL(19,4),
    period_start DATE,
    period_end DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 8.7 pricing_catalog
CREATE TABLE pricing_catalog (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    unit TEXT,
    price DECIMAL(19,4),
    effective_from DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pricing_catalog_effective ON pricing_catalog(effective_from);

-- DB.html 8.8 pricing_versions
CREATE TABLE pricing_versions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    pricing_catalog_id UUID,
    version INTEGER,
    price DECIMAL(19,4),
    effective_from DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 9.1 cost_protection_policies
CREATE TABLE cost_protection_policies (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    monthly_budget DECIMAL(19,4),
    daily_budget DECIMAL(19,4),
    alert_threshold DECIMAL(19,4),
    throttle_threshold DECIMAL(19,4),
    suspend_threshold DECIMAL(19,4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_cost_protection_tenant ON cost_protection_policies(tenant_id);

-- DB.html 9.2 cost_protection_alerts
CREATE TABLE cost_protection_alerts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    alert_type TEXT,
    current_usage DECIMAL(19,4),
    threshold DECIMAL(19,4),
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);


-- DB.html 9.3 cost_protection_actions
CREATE TABLE cost_protection_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    action_type TEXT,
    action_reason TEXT,
    triggered_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

