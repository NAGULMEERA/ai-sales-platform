-- Multi-dimension prompt resolution: industry / language / capability / preferred model.
ALTER TABLE prompt_template
    ADD COLUMN IF NOT EXISTS industry_code VARCHAR(64),
    ADD COLUMN IF NOT EXISTS language_code VARCHAR(16),
    ADD COLUMN IF NOT EXISTS capability VARCHAR(64),
    ADD COLUMN IF NOT EXISTS preferred_model VARCHAR(128);

CREATE INDEX IF NOT EXISTS idx_prompt_template_dimensions
    ON prompt_template (tenant_id, industry_code, capability, language_code)
    WHERE deleted_at IS NULL;

-- Backfill platform qualification seeds when present.
UPDATE prompt_template
SET industry_code = 'REAL_ESTATE',
    capability = 'LEAD_QUALIFICATION',
    language_code = 'en'
WHERE tenant_id = '00000000-0000-4000-8000-0000000000aa'
  AND code = 'LEAD_QUALIFY_REAL_ESTATE'
  AND deleted_at IS NULL;

UPDATE prompt_template
SET industry_code = 'AUTOMOBILE',
    capability = 'LEAD_QUALIFICATION',
    language_code = 'en'
WHERE tenant_id = '00000000-0000-4000-8000-0000000000aa'
  AND code = 'LEAD_QUALIFY_AUTOMOBILE'
  AND deleted_at IS NULL;
