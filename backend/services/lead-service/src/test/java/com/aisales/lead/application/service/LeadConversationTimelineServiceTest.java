package com.aisales.lead.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadConversationTimelineServiceTest {

    @Mock private LeadSideEffectRecorder sideEffects;
    @Mock private LeadRepository leadRepository;

    private LeadConversationTimelineService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        service = new LeadConversationTimelineService(sideEffects, leadRepository);
        tenantId = UUID.randomUUID();
        lenient().when(leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(any(), any()))
                .thenReturn(Optional.of(new Lead()));
    }

    @Test
    void shouldRecordConversationStartedActivity() {
        UUID leadId = UUID.randomUUID();
        String conversationId = UUID.randomUUID().toString();

        service.onConversationStarted(tenantId.toString(), leadId.toString(), conversationId, "WEB");

        verify(sideEffects).recordActivity(
                eq(leadId),
                eq("CONVERSATION_STARTED"),
                eq("Conversation " + conversationId + " started on WEB"),
                isNull());
    }

    @Test
    void shouldSkipWhenLeadIdMissing() {
        service.onConversationStarted(tenantId.toString(), null, UUID.randomUUID().toString(), "WEB");
        verifyNoInteractions(sideEffects);
    }

    @Test
    void shouldSkipWhenLeadDoesNotBelongToEventTenant() {
        UUID leadId = UUID.randomUUID();
        when(leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, leadId))
                .thenReturn(Optional.empty());

        service.onConversationStarted(tenantId.toString(), leadId.toString(), UUID.randomUUID().toString(), "WEB");

        verifyNoInteractions(sideEffects);
    }

    @Test
    void shouldSkipWhenTenantIdMissing() {
        service.onConversationStarted(null, UUID.randomUUID().toString(), UUID.randomUUID().toString(), "WEB");
        verifyNoInteractions(sideEffects);
    }
}
