-- Sprint 3: industry pipeline template codes + happy-path labels (metadata only).

UPDATE plugin_catalog
SET
    default_config = default_config
        || '{"defaultPipelineCode":"REAL_ESTATE_SALES_V1","pipelineHappyPath":["New","Qualified","Visit","Negotiation","Booked"]}'::jsonb
WHERE plugin_key = 'real-estate';

UPDATE plugin_catalog
SET
    default_config = default_config
        || '{"defaultPipelineCode":"AUTOMOBILE_SALES_V1","pipelineHappyPath":["New","Qualified","Test Drive","Quotation","Finance","Booked"]}'::jsonb
WHERE plugin_key = 'automobile';
