-- Align catalog config_schema with industry plugin descriptors and backfill installs
-- that predate V006 qualification keys.

UPDATE plugin_catalog
SET config_schema_json = '{"type":"object","properties":{"defaultPipelineCode":{"type":"string"},"pipelineTemplate":{"type":"object"},"catalogAttributeKeys":{"type":"array","items":{"type":"string"}},"matchAttributeKeys":{"type":"array","items":{"type":"string"}},"catalogOfferCategory":{"type":"string"},"quoteLineSource":{"type":"string"},"leadAttributeKeys":{"type":"array","items":{"type":"string"}},"qualificationPromptCode":{"type":"string"},"qualificationVariableKeys":{"type":"array","items":{"type":"string"}},"conversationFollowupWorkflowKey":{"type":"string"},"defaultFollowupType":{"type":"string"},"conversationSubjectTemplate":{"type":"string"},"inPersonEngagementLabel":{"type":"string"}}}'
WHERE plugin_key IN ('real-estate', 'automobile');

UPDATE plugin_installation AS i
SET
    config = i.config
        || jsonb_strip_nulls(jsonb_build_object(
            'qualificationPromptCode',
            CASE
                WHEN COALESCE(i.config->>'qualificationPromptCode', '') = ''
                    THEN c.default_config->>'qualificationPromptCode'
                ELSE NULL
            END,
            'qualificationVariableKeys',
            CASE
                WHEN i.config->'qualificationVariableKeys' IS NULL
                    OR jsonb_typeof(i.config->'qualificationVariableKeys') <> 'array'
                    OR jsonb_array_length(i.config->'qualificationVariableKeys') = 0
                    THEN c.default_config->'qualificationVariableKeys'
                ELSE NULL
            END
        )),
    updated_at = NOW()
FROM plugin_catalog AS c
WHERE i.plugin_key = c.plugin_key
  AND c.plugin_key IN ('real-estate', 'automobile');
