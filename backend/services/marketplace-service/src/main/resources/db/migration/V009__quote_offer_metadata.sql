-- Sprint 5: quote line items consume catalog.offerId (no industry quote types).

UPDATE plugin_catalog
SET
    capabilities = COALESCE(capabilities, '[]'::jsonb) || '["deal.quote"]'::jsonb,
    default_config = default_config
        || '{"catalogOfferCategory":"residential","quoteLineSource":"catalog.offerId"}'::jsonb,
    metadata = COALESCE(metadata, '{}'::jsonb) || '{"industryQuoteType":false}'::jsonb
WHERE plugin_key = 'real-estate';

UPDATE plugin_catalog
SET
    capabilities = COALESCE(capabilities, '[]'::jsonb) || '["deal.quote"]'::jsonb,
    default_config = default_config
        || '{"catalogOfferCategory":"vehicle","quoteLineSource":"catalog.offerId"}'::jsonb,
    metadata = COALESCE(metadata, '{}'::jsonb) || '{"industryQuoteType":false}'::jsonb
WHERE plugin_key = 'automobile';
