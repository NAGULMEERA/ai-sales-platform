-- Sprint 1 dual-industry vertical validation:
-- seed automobile industry plugin; add leadAttributeKeys to real-estate defaults.

INSERT INTO plugin_catalog (
    plugin_key, plugin_type, version, display_name, description,
    capabilities, industry_code, config_schema_json, default_config, metadata
) VALUES
(
    'automobile',
    'INDUSTRY',
    '1.0.0',
    'Automobile Industry',
    'Industry metadata: suggested catalog/lead attribute keys and pipeline template references. No industry microservice.',
    '["industry.automobile","catalog.attributes","pipeline.template"]'::jsonb,
    'AUTOMOBILE',
    '{"type":"object","properties":{"defaultPipelineCode":{"type":"string"},"catalogAttributeKeys":{"type":"array","items":{"type":"string"}},"leadAttributeKeys":{"type":"array","items":{"type":"string"}}}}',
    '{"defaultPipelineCode":"DEFAULT_SALES_V1","catalogAttributeKeys":["make","model","year","price"],"leadAttributeKeys":["vehicle","budget","financeRequired"]}'::jsonb,
    '{"ownsMicroservice":false,"leadSubtype":false}'::jsonb
);

UPDATE plugin_catalog
SET
    config_schema_json = '{"type":"object","properties":{"defaultPipelineCode":{"type":"string"},"catalogAttributeKeys":{"type":"array","items":{"type":"string"}},"leadAttributeKeys":{"type":"array","items":{"type":"string"}}}}',
    default_config = jsonb_set(
        COALESCE(default_config, '{}'::jsonb),
        '{leadAttributeKeys}',
        '["budget","location","propertyType"]'::jsonb,
        true
    )
WHERE plugin_key = 'real-estate';
