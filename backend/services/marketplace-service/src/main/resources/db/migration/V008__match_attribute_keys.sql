-- Sprint 4: catalog match attribute keys (metadata for dual-industry matching).

UPDATE plugin_catalog
SET
    capabilities = COALESCE(capabilities, '[]'::jsonb) || '["catalog.match"]'::jsonb,
    default_config = default_config
        || '{"matchAttributeKeys":["bedrooms","bathrooms","location"]}'::jsonb
WHERE plugin_key = 'real-estate';

UPDATE plugin_catalog
SET
    capabilities = COALESCE(capabilities, '[]'::jsonb) || '["catalog.match"]'::jsonb,
    default_config = default_config
        || '{"matchAttributeKeys":["make","model","year"]}'::jsonb
WHERE plugin_key = 'automobile';
