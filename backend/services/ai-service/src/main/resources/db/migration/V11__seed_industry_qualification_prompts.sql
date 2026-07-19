-- Platform-seeded industry lead-qualification prompts.
-- Owned by ai-service prompt registry (not identity tenants).
-- PromptService.resolveForExecution falls back to PLATFORM_TENANT_ID when a tenant has no override.

-- Real Estate
INSERT INTO prompt_template (
    id, tenant_id, code, name, purpose, status, active_version, created_at, updated_at, version
)
SELECT
    'a1000000-0000-4000-8000-000000000001'::uuid,
    '00000000-0000-4000-8000-0000000000aa'::uuid,
    'LEAD_QUALIFY_REAL_ESTATE',
    'Real Estate Lead Qualification',
    'LEAD_QUALIFICATION',
    'ACTIVE',
    1,
    NOW(),
    NOW(),
    0
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_template
    WHERE tenant_id = '00000000-0000-4000-8000-0000000000aa'::uuid
      AND code = 'LEAD_QUALIFY_REAL_ESTATE'
      AND deleted_at IS NULL
);

INSERT INTO prompt_version (
    id, tenant_id, prompt_id, version_number, system_template, user_template,
    variables, expected_output_hint, changelog, status, created_at
)
SELECT
    'a1000000-0000-4000-8000-000000000011'::uuid,
    '00000000-0000-4000-8000-0000000000aa'::uuid,
    'a1000000-0000-4000-8000-000000000001'::uuid,
    1,
    'You are a real-estate lead qualifier. Return JSON only with recommendation (QUALIFY|REVIEW|DISQUALIFY), score (0-100), and a short rationale.',
    'Qualify RE lead {{leadName}} budget={{budget}} location={{location}} timeline={{timeline}}',
    '["leadName","budget","location","timeline"]'::jsonb,
    '{"recommendation":"QUALIFY|REVIEW|DISQUALIFY","score":0,"rationale":"string"}',
    'Platform seed v1',
    'ACTIVE',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_version
    WHERE tenant_id = '00000000-0000-4000-8000-0000000000aa'::uuid
      AND prompt_id = 'a1000000-0000-4000-8000-000000000001'::uuid
      AND version_number = 1
);

-- Automobile
INSERT INTO prompt_template (
    id, tenant_id, code, name, purpose, status, active_version, created_at, updated_at, version
)
SELECT
    'a1000000-0000-4000-8000-000000000002'::uuid,
    '00000000-0000-4000-8000-0000000000aa'::uuid,
    'LEAD_QUALIFY_AUTOMOBILE',
    'Automobile Lead Qualification',
    'LEAD_QUALIFICATION',
    'ACTIVE',
    1,
    NOW(),
    NOW(),
    0
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_template
    WHERE tenant_id = '00000000-0000-4000-8000-0000000000aa'::uuid
      AND code = 'LEAD_QUALIFY_AUTOMOBILE'
      AND deleted_at IS NULL
);

INSERT INTO prompt_version (
    id, tenant_id, prompt_id, version_number, system_template, user_template,
    variables, expected_output_hint, changelog, status, created_at
)
SELECT
    'a1000000-0000-4000-8000-000000000012'::uuid,
    '00000000-0000-4000-8000-0000000000aa'::uuid,
    'a1000000-0000-4000-8000-000000000002'::uuid,
    1,
    'You are an automobile lead qualifier. Return JSON only with recommendation (QUALIFY|REVIEW|DISQUALIFY), score (0-100), and a short rationale.',
    'Qualify Auto lead {{leadName}} budget={{budget}} vehicle={{vehicle}} finance={{financeRequired}} exchange={{exchange}}',
    '["leadName","budget","vehicle","financeRequired","exchange"]'::jsonb,
    '{"recommendation":"QUALIFY|REVIEW|DISQUALIFY","score":0,"rationale":"string"}',
    'Platform seed v1',
    'ACTIVE',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_version
    WHERE tenant_id = '00000000-0000-4000-8000-0000000000aa'::uuid
      AND prompt_id = 'a1000000-0000-4000-8000-000000000002'::uuid
      AND version_number = 1
);
