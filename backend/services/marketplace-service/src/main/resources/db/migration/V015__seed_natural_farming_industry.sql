-- Natural Farming industry vertical (production-ready metadata plugin).
-- Platform Core unchanged: Lead / Catalog / Deal / AI / Workflow / Billing / Search / Analytics.

INSERT INTO plugin_catalog (
    plugin_key, plugin_type, version, min_platform_version, display_name, description,
    capabilities, industry_code, config_schema_json, default_config, metadata
) VALUES
(
    'natural-farming',
    'INDUSTRY',
    '1.0.0',
    '1.0.0',
    'Natural Farming Industry',
    'Industry metadata for natural farming: farm/harvest/catalog attributes, sales pipeline, AI qualification & recommendation prompt refs, WhatsApp follow-up. No industry microservice.',
    '["industry.natural_farming","catalog.attributes","catalog.match","catalog.recommend","pipeline.template","deal.quote","conversation.followup","notification.whatsapp","workflow.order","billing.payment","search.projection","analytics.facts"]'::jsonb,
    'NATURAL_FARMING',
    '{"type":"object","properties":{"defaultPipelineCode":{"type":"string"},"pipelineTemplate":{"type":"object"},"catalogAttributeKeys":{"type":"array","items":{"type":"string"}},"matchAttributeKeys":{"type":"array","items":{"type":"string"}},"catalogOfferCategory":{"type":"string"},"quoteLineSource":{"type":"string"},"leadAttributeKeys":{"type":"array","items":{"type":"string"}},"qualificationPromptCode":{"type":"string"},"qualificationVariableKeys":{"type":"array","items":{"type":"string"}},"recommendationPromptCode":{"type":"string"},"conversationFollowupWorkflowKey":{"type":"string"},"defaultFollowupType":{"type":"string"},"conversationSubjectTemplate":{"type":"string"},"inPersonEngagementLabel":{"type":"string"},"orderWorkflowKey":{"type":"string"},"paymentCapabilityRef":{"type":"string"},"whatsappLeadCaptureEnabled":{"type":"boolean"},"knowledgeCategoryCode":{"type":"string"},"farmAttributeKeys":{"type":"array","items":{"type":"string"}},"harvestAttributeKeys":{"type":"array","items":{"type":"string"}},"inventoryAttributeKeys":{"type":"array","items":{"type":"string"}},"deliveryAttributeKeys":{"type":"array","items":{"type":"string"}}}}',
    '{
      "defaultPipelineCode": "NATURAL_FARMING_SALES_V1",
      "pipelineHappyPath": ["New", "Qualified", "Farm Visit", "Negotiation", "Order Confirmed", "Delivered"],
      "pipelineTemplate": {
        "code": "NATURAL_FARMING_SALES_V1",
        "name": "Natural Farming Sales Pipeline",
        "description": "Natural farming journey using LeadStatus codes",
        "stages": [
          {"status": "NEW", "displayName": "New", "order": 10, "terminal": false},
          {"status": "CONTACTED", "displayName": "Contacted", "order": 20, "terminal": false},
          {"status": "QUALIFIED", "displayName": "Qualified", "order": 30, "terminal": false},
          {"status": "APPOINTMENT_BOOKED", "displayName": "Farm Visit Scheduled", "order": 40, "terminal": false},
          {"status": "VISITED", "displayName": "Farm Visit", "order": 50, "terminal": false},
          {"status": "NEGOTIATING", "displayName": "Negotiation", "order": 60, "terminal": false},
          {"status": "WON", "displayName": "Order Confirmed / Delivered", "order": 70, "terminal": true},
          {"status": "LOST", "displayName": "Lost", "order": 80, "terminal": true},
          {"status": "ARCHIVED", "displayName": "Archived", "order": 90, "terminal": true}
        ],
        "transitions": [
          {"from": "NEW", "to": ["CONTACTED", "QUALIFIED", "LOST"]},
          {"from": "CONTACTED", "to": ["QUALIFIED", "LOST"]},
          {"from": "QUALIFIED", "to": ["APPOINTMENT_BOOKED", "VISITED", "LOST"]},
          {"from": "APPOINTMENT_BOOKED", "to": ["VISITED", "LOST"]},
          {"from": "VISITED", "to": ["NEGOTIATING", "LOST"]},
          {"from": "NEGOTIATING", "to": ["WON", "LOST"]},
          {"from": "WON", "to": ["ARCHIVED"]},
          {"from": "LOST", "to": ["ARCHIVED"]},
          {"from": "ARCHIVED", "to": []}
        ]
      },
      "catalogAttributeKeys": ["farmId","farmName","plotId","cropType","variety","season","harvestDate","yieldKg","unit","organicCertified","certificationBody","region","packaging","shelfLifeDays","stockKg","availableFrom","pricePerKg"],
      "matchAttributeKeys": ["cropType","region","organicCertified","season"],
      "catalogOfferCategory": "natural-produce",
      "quoteLineSource": "catalog.offerId",
      "farmAttributeKeys": ["farmId","farmName","plotId","soilType","irrigationType","region"],
      "harvestAttributeKeys": ["cropType","variety","season","harvestDate","yieldKg","unit"],
      "inventoryAttributeKeys": ["stockKg","availableFrom","shelfLifeDays","packaging"],
      "deliveryAttributeKeys": ["deliveryRegion","deliveryWindow","carrierRef","deliveredAt"],
      "leadAttributeKeys": ["buyerType","cropInterest","volumeKg","deliveryRegion","organicRequired","budget","harvestWindow"],
      "qualificationPromptCode": "LEAD_QUALIFY_NATURAL_FARMING",
      "qualificationVariableKeys": ["budget","cropInterest","volumeKg","organicRequired","deliveryRegion"],
      "recommendationPromptCode": "CATALOG_RECOMMEND_NATURAL_FARMING",
      "conversationFollowupWorkflowKey": "CONVERSATION_FOLLOWUP_V1",
      "defaultFollowupType": "DELIVERY_FOLLOWUP",
      "conversationSubjectTemplate": "Harvest / delivery follow-up",
      "inPersonEngagementLabel": "Farm visit",
      "orderWorkflowKey": "LEAD_LIFECYCLE_V1",
      "paymentCapabilityRef": "billing-service",
      "whatsappLeadCaptureEnabled": true,
      "knowledgeCategoryCode": "NATURAL_FARMING_FAQ"
    }'::jsonb,
    '{"ownsMicroservice":false,"leadSubtype":false,"industryQuoteType":false,"industryConversationType":false}'::jsonb
)
ON CONFLICT (plugin_key) DO NOTHING;
