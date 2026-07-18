package com.aisales.lead.application.service;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadConversationTimelineServiceTest {

    @Mock private LeadSideEffectRecorder sideEffects;

    private LeadConversationTimelineService service;

    @BeforeEach
    void setUp() {
        service = new LeadConversationTimelineService(sideEffects);
    }

    @Test
    void shouldRecordConversationStartedActivity() {
        UUID leadId = UUID.randomUUID();
        String conversationId = UUID.randomUUID().toString();

        service.onConversationStarted(leadId.toString(), conversationId, "WEB");

        verify(sideEffects).recordActivity(
                eq(leadId),
                eq("CONVERSATION_STARTED"),
                eq("Conversation " + conversationId + " started on WEB"),
                isNull());
    }

    @Test
    void shouldSkipWhenLeadIdMissing() {
        service.onConversationStarted(null, UUID.randomUUID().toString(), "WEB");
        verifyNoInteractions(sideEffects);
    }
}
