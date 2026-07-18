-- Sprint 2: qualification prompt codes + variable keys for dual-industry vertical validation.

UPDATE plugin_catalog
SET
    config_schema_json = '{"type":"object","properties":{"defaultPipelineCode":{"type":"string"},"catalogAttributeKeys":{"type":"array","items":{"type":"string"}},"leadAttributeKeys":{"type":"array","items":{"type":"string"}},"qualificationPromptCode":{"type":"string"},"qualificationVariableKeys":{"type":"array","items":{"type":"string"}}}}',
    default_config = default_config
        || '{"leadAttributeKeys":["budget","location","propertyType","timeline"],"qualificationPromptCode":"LEAD_QUALIFY_REAL_ESTATE","qualificationVariableKeys":["budget","location","timeline"]}'::jsonb
WHERE plugin_key = 'real-estate';

UPDATE plugin_catalog
SET
    config_schema_json = '{"type":"object","properties":{"defaultPipelineCode":{"type":"string"},"catalogAttributeKeys":{"type":"array","items":{"type":"string"}},"leadAttributeKeys":{"type":"array","items":{"type":"string"}},"qualificationPromptCode":{"type":"string"},"qualificationVariableKeys":{"type":"array","items":{"type":"string"}}}}',
    default_config = default_config
        || '{"leadAttributeKeys":["vehicle","budget","financeRequired","exchange"],"qualificationPromptCode":"LEAD_QUALIFY_AUTOMOBILE","qualificationVariableKeys":["budget","vehicle","financeRequired","exchange"]}'::jsonb
WHERE plugin_key = 'automobile';
