-- Natural Farming industry prompts (platform tenant seed).
-- AI recommends; Lead / Catalog remain authoritative.

INSERT INTO prompt_template (
    id, tenant_id, code, name, purpose, status, active_version,
    industry_code, capability, language_code,
    created_at, updated_at, version
)
SELECT
    'a1000000-0000-4000-8000-000000000003'::uuid,
    '00000000-0000-4000-8000-0000000000aa'::uuid,
    'LEAD_QUALIFY_NATURAL_FARMING',
    'Natural Farming Lead Qualification',
    'LEAD_QUALIFICATION',
    'ACTIVE',
    1,
    'NATURAL_FARMING',
    'LEAD_QUALIFICATION',
    'en',
    NOW(),
    NOW(),
    0
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_template
    WHERE tenant_id = '00000000-0000-4000-8000-0000000000aa'::uuid
      AND code = 'LEAD_QUALIFY_NATURAL_FARMING'
      AND deleted_at IS NULL
);

INSERT INTO prompt_version (
    id, tenant_id, prompt_id, version_number, system_template, user_template,
    variables, expected_output_hint, changelog, status, created_at
)
SELECT
    'a1000000-0000-4000-8000-000000000013'::uuid,
    '00000000-0000-4000-8000-0000000000aa'::uuid,
    'a1000000-0000-4000-8000-000000000003'::uuid,
    1,
    'You are a natural-farming lead qualifier for produce buyers (retailers, restaurants, consumers). Return JSON only with recommendation (QUALIFY|REVIEW|DISQUALIFY), score (0-100), and a short rationale. Prefer organic/certification fit and delivery region realism.',
    'Qualify natural-farming lead {{leadName}} budget={{budget}} cropInterest={{cropInterest}} volumeKg={{volumeKg}} organicRequired={{organicRequired}} deliveryRegion={{deliveryRegion}}',
    '["leadName","budget","cropInterest","volumeKg","organicRequired","deliveryRegion"]'::jsonb,
    '{"recommendation":"QUALIFY|REVIEW|DISQUALIFY","score":0,"rationale":"string"}',
    'Natural farming platform seed v1',
    'ACTIVE',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_version
    WHERE tenant_id = '00000000-0000-4000-8000-0000000000aa'::uuid
      AND prompt_id = 'a1000000-0000-4000-8000-000000000003'::uuid
      AND version_number = 1
);

INSERT INTO prompt_template (
    id, tenant_id, code, name, purpose, status, active_version,
    industry_code, capability, language_code,
    created_at, updated_at, version
)
SELECT
    'a1000000-0000-4000-8000-000000000004'::uuid,
    '00000000-0000-4000-8000-0000000000aa'::uuid,
    'CATALOG_RECOMMEND_NATURAL_FARMING',
    'Natural Farming Catalog Recommendation',
    'CATALOG_RECOMMENDATION',
    'ACTIVE',
    1,
    'NATURAL_FARMING',
    'CATALOG_RECOMMENDATION',
    'en',
    NOW(),
    NOW(),
    0
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_template
    WHERE tenant_id = '00000000-0000-4000-8000-0000000000aa'::uuid
      AND code = 'CATALOG_RECOMMEND_NATURAL_FARMING'
      AND deleted_at IS NULL
);

INSERT INTO prompt_version (
    id, tenant_id, prompt_id, version_number, system_template, user_template,
    variables, expected_output_hint, changelog, status, created_at
)
SELECT
    'a1000000-0000-4000-8000-000000000014'::uuid,
    '00000000-0000-4000-8000-0000000000aa'::uuid,
    'a1000000-0000-4000-8000-000000000004'::uuid,
    1,
    'You rank natural-farming produce offers. Return JSON only with rankedOfferCodes (array of catalog offer codes), confidence (0-100), and rationale. Prefer cropType, organicCertified, season, and region fit. Do not invent offers not present in candidates.',
    'Recommend produce for buyer cropInterest={{cropInterest}} volumeKg={{volumeKg}} organicRequired={{organicRequired}} region={{deliveryRegion}} candidates={{candidates}}',
    '["cropInterest","volumeKg","organicRequired","deliveryRegion","candidates"]'::jsonb,
    '{"rankedOfferCodes":["string"],"confidence":0,"rationale":"string"}',
    'Natural farming recommendation seed v1',
    'ACTIVE',
    NOW()
WHERE NOT EXISTS (
    SELECT 1 FROM prompt_version
    WHERE tenant_id = '00000000-0000-4000-8000-0000000000aa'::uuid
      AND prompt_id = 'a1000000-0000-4000-8000-000000000004'::uuid
      AND version_number = 1
);
