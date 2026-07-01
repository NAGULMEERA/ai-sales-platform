-- ============================================================================
-- V029__seed_data.sql
-- Reference seed data (DB.html v7 sample alignment)
-- ============================================================================

INSERT INTO permissions (id, code, name, description, category)
SELECT gen_random_uuid(), r.code, r.name, r.description, r.category
FROM (VALUES
    ('lead:read', 'Read leads', 'Read lead records', 'lead'),
    ('lead:create', 'Create leads', 'Create lead records', 'lead'),
    ('lead:update', 'Update leads', 'Update lead records', 'lead'),
    ('lead:delete', 'Delete leads', 'Delete lead records', 'lead'),
    ('customer:read', 'Read customers', 'Read customer records', 'customer'),
    ('appointment:read', 'Read appointments', 'Read appointment records', 'appointment')
) AS r(code, name, description, category)
WHERE NOT EXISTS (SELECT 1 FROM permissions p WHERE p.code = r.code);

INSERT INTO plans (id, name, price_monthly, price_annual, features, limits, trial_days)
SELECT gen_random_uuid(), p.name, p.price_monthly, p.price_annual, p.features::jsonb, p.limits::jsonb, p.trial_days
FROM (VALUES
    ('Starter', 2999.00, 29990.00, '{"leads": 100}', '{"users": 5}', 14),
    ('Pro', 5999.00, 59990.00, '{"leads": 500}', '{"users": 25}', 14),
    ('Enterprise', 12999.00, 129990.00, '{"leads": "unlimited"}', '{"users": "unlimited"}', 7)
) AS p(name, price_monthly, price_annual, features, limits, trial_days)
WHERE NOT EXISTS (SELECT 1 FROM plans pl WHERE pl.name = p.name);
