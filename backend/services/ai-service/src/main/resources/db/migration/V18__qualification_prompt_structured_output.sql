-- Enrich platform qualification prompts with production structured-output contract.

UPDATE prompt_version
SET
    system_template = 'You are a lead qualification assistant. Return JSON only with: '
        || 'qualificationScore (0-100), confidenceScore (0-100), recommendation (QUALIFY|REVIEW|DISQUALIFY), '
        || 'reasoning (string), recommendedProduct (string|null), recommendedSalesAction (string), '
        || 'riskFactors (string[]), missingInformation (string[]), followUpQuestions (string[]).',
    expected_output_hint = '{"qualificationScore":0,"confidenceScore":0,"recommendation":"QUALIFY|REVIEW|DISQUALIFY",'
        || '"reasoning":"string","recommendedProduct":"string","recommendedSalesAction":"string",'
        || '"riskFactors":[],"missingInformation":[],"followUpQuestions":[]}',
    changelog = coalesce(changelog, '') || ' | structured qualification output v2'
WHERE tenant_id = '00000000-0000-4000-8000-0000000000aa'::uuid
  AND prompt_id IN (
      'a1000000-0000-4000-8000-000000000001'::uuid,
      'a1000000-0000-4000-8000-000000000002'::uuid
  )
  AND version_number = 1;
