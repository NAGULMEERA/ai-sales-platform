-- Track B: persist full industry pipeline graphs on plugin catalog (metadata SoT).
-- Mirrors plugin classpath JSON: pipeline-templates/*_SALES_V1.json

UPDATE plugin_catalog
SET default_config = default_config || jsonb_build_object(
    'pipelineTemplate',
    '{
      "code":"REAL_ESTATE_SALES_V1",
      "name":"Real Estate Sales Pipeline",
      "description":"Real Estate journey: New → Qualified → Visit → Negotiation → Booked. Stage codes remain LeadStatus.",
      "stages":[
        {"status":"NEW","displayName":"New","order":10,"terminal":false},
        {"status":"CONTACTED","displayName":"Contacted","order":20,"terminal":false},
        {"status":"QUALIFIED","displayName":"Qualified","order":30,"terminal":false},
        {"status":"APPOINTMENT_BOOKED","displayName":"Site Visit Scheduled","order":40,"terminal":false},
        {"status":"VISITED","displayName":"Visit","order":50,"terminal":false},
        {"status":"NEGOTIATING","displayName":"Negotiation","order":60,"terminal":false},
        {"status":"WON","displayName":"Booked","order":70,"terminal":true},
        {"status":"LOST","displayName":"Lost","order":80,"terminal":true},
        {"status":"ARCHIVED","displayName":"Archived","order":90,"terminal":true}
      ],
      "transitions":[
        {"from":"NEW","to":["CONTACTED","QUALIFIED","LOST"]},
        {"from":"CONTACTED","to":["QUALIFIED","LOST"]},
        {"from":"QUALIFIED","to":["VISITED","LOST"]},
        {"from":"APPOINTMENT_BOOKED","to":["VISITED","LOST"]},
        {"from":"VISITED","to":["NEGOTIATING","LOST"]},
        {"from":"NEGOTIATING","to":["WON","LOST"]},
        {"from":"WON","to":["ARCHIVED"]},
        {"from":"LOST","to":["ARCHIVED"]},
        {"from":"ARCHIVED","to":[]}
      ]
    }'::jsonb
)
WHERE plugin_key = 'real-estate';

UPDATE plugin_catalog
SET default_config = default_config || jsonb_build_object(
    'pipelineTemplate',
    '{
      "code":"AUTOMOBILE_SALES_V1",
      "name":"Automobile Sales Pipeline",
      "description":"Automobile journey: New → Qualified → Test Drive → Quotation → Finance → Booked. Stage codes remain LeadStatus.",
      "stages":[
        {"status":"NEW","displayName":"New","order":10,"terminal":false},
        {"status":"CONTACTED","displayName":"Contacted","order":20,"terminal":false},
        {"status":"QUALIFIED","displayName":"Qualified","order":30,"terminal":false},
        {"status":"APPOINTMENT_BOOKED","displayName":"Test Drive","order":40,"terminal":false},
        {"status":"VISITED","displayName":"Quotation","order":50,"terminal":false},
        {"status":"NEGOTIATING","displayName":"Finance","order":60,"terminal":false},
        {"status":"WON","displayName":"Booked","order":70,"terminal":true},
        {"status":"LOST","displayName":"Lost","order":80,"terminal":true},
        {"status":"ARCHIVED","displayName":"Archived","order":90,"terminal":true}
      ],
      "transitions":[
        {"from":"NEW","to":["CONTACTED","QUALIFIED","LOST"]},
        {"from":"CONTACTED","to":["QUALIFIED","LOST"]},
        {"from":"QUALIFIED","to":["APPOINTMENT_BOOKED","LOST"]},
        {"from":"APPOINTMENT_BOOKED","to":["VISITED","LOST"]},
        {"from":"VISITED","to":["NEGOTIATING","LOST"]},
        {"from":"NEGOTIATING","to":["WON","LOST"]},
        {"from":"WON","to":["ARCHIVED"]},
        {"from":"LOST","to":["ARCHIVED"]},
        {"from":"ARCHIVED","to":[]}
      ]
    }'::jsonb
)
WHERE plugin_key = 'automobile';
