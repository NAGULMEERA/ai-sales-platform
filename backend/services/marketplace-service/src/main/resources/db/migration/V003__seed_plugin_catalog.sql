-- Seed catalog from Phase 5 plugin stubs (metadata only; runtime send stays in notification-service).

INSERT INTO plugin_catalog (
    plugin_key, plugin_type, version, display_name, description,
    capabilities, industry_code, config_schema_json, default_config, metadata
) VALUES
(
    'email-channel',
    'CAPABILITY',
    '1.0.0',
    'Email Channel',
    'Capability metadata for email notification delivery. SMTP remains in notification-service.',
    '["notification.email"]'::jsonb,
    NULL,
    '{"type":"object","properties":{"fromAddress":{"type":"string"},"replyTo":{"type":"string"}},"required":["fromAddress"]}',
    '{"fromAddress":"","replyTo":""}'::jsonb,
    '{"runtimeOwner":"notification-service","implementsSend":false}'::jsonb
),
(
    'whatsapp-channel',
    'CAPABILITY',
    '1.0.0',
    'WhatsApp Channel',
    'Capability metadata for WhatsApp notifications. Meta Cloud API remains outside Platform Core plugins.',
    '["notification.whatsapp"]'::jsonb,
    NULL,
    '{"type":"object","properties":{"phoneNumberId":{"type":"string"},"wabaId":{"type":"string"}},"required":["phoneNumberId"]}',
    '{"phoneNumberId":"","wabaId":""}'::jsonb,
    '{"runtimeOwner":"notification-service","implementsSend":false}'::jsonb
),
(
    'real-estate',
    'INDUSTRY',
    '1.0.0',
    'Real Estate Industry',
    'Industry metadata: suggested catalog attribute keys and pipeline template references. No industry microservice.',
    '["industry.real_estate","catalog.attributes","pipeline.template"]'::jsonb,
    'REAL_ESTATE',
    '{"type":"object","properties":{"defaultPipelineCode":{"type":"string"},"catalogAttributeKeys":{"type":"array","items":{"type":"string"}}}}',
    '{"defaultPipelineCode":"DEFAULT_SALES_V1","catalogAttributeKeys":["bedrooms","bathrooms","location","price"]}'::jsonb,
    '{"ownsMicroservice":false,"leadSubtype":false}'::jsonb
);
