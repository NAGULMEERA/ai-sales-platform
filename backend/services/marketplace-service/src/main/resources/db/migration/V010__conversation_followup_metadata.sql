-- Sprint 6: conversation + follow-up metadata (same workflow key; different followup types).

UPDATE plugin_catalog
SET
    capabilities = COALESCE(capabilities, '[]'::jsonb) || '["conversation.followup"]'::jsonb,
    default_config = default_config || '{
        "conversationFollowupWorkflowKey":"CONVERSATION_FOLLOWUP_V1",
        "defaultFollowupType":"VISIT_FOLLOWUP",
        "conversationSubjectTemplate":"Visit follow-up",
        "inPersonEngagementLabel":"Site visit"
    }'::jsonb,
    metadata = COALESCE(metadata, '{}'::jsonb) || '{"industryConversationType":false}'::jsonb
WHERE plugin_key = 'real-estate';

UPDATE plugin_catalog
SET
    capabilities = COALESCE(capabilities, '[]'::jsonb) || '["conversation.followup"]'::jsonb,
    default_config = default_config || '{
        "conversationFollowupWorkflowKey":"CONVERSATION_FOLLOWUP_V1",
        "defaultFollowupType":"TEST_DRIVE_FOLLOWUP",
        "conversationSubjectTemplate":"Test-drive follow-up",
        "inPersonEngagementLabel":"Test drive"
    }'::jsonb,
    metadata = COALESCE(metadata, '{}'::jsonb) || '{"industryConversationType":false}'::jsonb
WHERE plugin_key = 'automobile';
