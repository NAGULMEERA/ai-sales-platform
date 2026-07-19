package com.aisales.conversation.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.conversation.AddMessageRequest;
import com.aisales.common.contracts.conversation.ConversationChannel;
import com.aisales.common.contracts.conversation.ConversationDto;
import com.aisales.common.contracts.conversation.ConversationStatus;
import com.aisales.common.contracts.conversation.CreateConversationRequest;
import com.aisales.common.contracts.conversation.MessageDeliveryStatus;
import com.aisales.common.contracts.conversation.MessageDirection;
import com.aisales.common.contracts.conversation.MessageSenderType;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.ConversationStartedEvent;
import com.aisales.common.events.model.MessageReceivedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.conversation.application.audit.ConversationAuditor;
import com.aisales.conversation.application.mapper.ConversationMapper;
import com.aisales.conversation.domain.channel.OutboundChannelPort;
import com.aisales.conversation.domain.entity.ConversationMessage;
import com.aisales.conversation.domain.entity.ConversationParticipant;
import com.aisales.conversation.domain.entity.ConversationThread;
import com.aisales.conversation.domain.entity.ConversationTimelineEntry;
import com.aisales.conversation.infrastructure.persistence.ConversationAttachmentRepository;
import com.aisales.conversation.infrastructure.persistence.ConversationMessageRepository;
import com.aisales.conversation.infrastructure.persistence.ConversationParticipantRepository;
import com.aisales.conversation.infrastructure.persistence.ConversationThreadRepository;
import com.aisales.conversation.infrastructure.persistence.ConversationTimelineEntryRepository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock private ConversationThreadRepository threadRepository;
    @Mock private ConversationMessageRepository messageRepository;
    @Mock private ConversationParticipantRepository participantRepository;
    @Mock private ConversationAttachmentRepository attachmentRepository;
    @Mock private ConversationTimelineEntryRepository timelineRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private ConversationAuditor auditor;
    @Mock private OutboundChannelPort outboundChannelPort;
    @Mock private ObjectProvider<com.aisales.common.observability.metrics.PlatformMetrics> platformMetrics;

    private ConversationService conversationService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        org.mockito.Mockito.lenient().when(outboundChannelPort.supports(any())).thenReturn(true);
        conversationService = new ConversationService(
                threadRepository,
                messageRepository,
                participantRepository,
                attachmentRepository,
                timelineRepository,
                new ConversationMapper(),
                eventPublisher,
                auditor,
                List.of(outboundChannelPort),
                platformMetrics);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldStartConversationAndPublishEvent() {
        UUID leadId = UUID.randomUUID();
        when(threadRepository.saveAndFlush(any(ConversationThread.class))).thenAnswer(inv -> {
            ConversationThread t = inv.getArgument(0);
            t.setId(UUID.randomUUID());
            return t;
        });
        when(participantRepository.save(any(ConversationParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timelineRepository.save(any(ConversationTimelineEntry.class))).thenAnswer(inv -> inv.getArgument(0));

        ConversationDto dto = conversationService.start(CreateConversationRequest.builder()
                .leadId(leadId)
                .channel(ConversationChannel.WHATSAPP)
                .subject("Inquiry")
                .build());

        assertThat(dto.getLeadId()).isEqualTo(leadId);
        assertThat(dto.getStatus()).isEqualTo(ConversationStatus.OPEN);
        ArgumentCaptor<ConversationStartedEvent> captor =
                ArgumentCaptor.forClass(ConversationStartedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("ConversationStarted");
        assertThat(captor.getValue().getChannel()).isEqualTo("WHATSAPP");
    }

    @Test
    void shouldRequireLeadOrCustomer() {
        assertThatThrownBy(() -> conversationService.start(CreateConversationRequest.builder()
                        .channel(ConversationChannel.WEB)
                        .build()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("leadId or customerId");
    }

    @Test
    void shouldRejectMessageOnClosedConversation() {
        UUID conversationId = UUID.randomUUID();
        ConversationThread closed = ConversationThread.builder()
                .id(conversationId)
                .tenantId(tenantId)
                .leadId(UUID.randomUUID())
                .channel(ConversationChannel.WEB)
                .status(ConversationStatus.CLOSED)
                .build();
        when(threadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, conversationId))
                .thenReturn(Optional.of(closed));

        assertThatThrownBy(() -> conversationService.addMessage(conversationId, AddMessageRequest.builder()
                        .senderType(MessageSenderType.AGENT)
                        .body("hello")
                        .build()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("closed");
    }

    @Test
    void shouldAddInboundMessageAndPublishMessageReceived() {
        UUID conversationId = UUID.randomUUID();
        ConversationThread open = ConversationThread.builder()
                .id(conversationId)
                .tenantId(tenantId)
                .leadId(UUID.randomUUID())
                .channel(ConversationChannel.WEB)
                .status(ConversationStatus.OPEN)
                .build();
        when(threadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, conversationId))
                .thenReturn(Optional.of(open));
        when(messageRepository.saveAndFlush(any(ConversationMessage.class))).thenAnswer(inv -> {
            ConversationMessage m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });
        when(messageRepository.save(any(ConversationMessage.class))).thenAnswer(inv -> inv.getArgument(0));
        when(threadRepository.save(any(ConversationThread.class))).thenAnswer(inv -> inv.getArgument(0));
        when(timelineRepository.save(any(ConversationTimelineEntry.class))).thenAnswer(inv -> inv.getArgument(0));
        when(attachmentRepository.findByTenantIdAndMessageIdOrderByCreatedAtAsc(any(), any()))
                .thenReturn(List.of());

        var message = conversationService.addMessage(conversationId, AddMessageRequest.builder()
                .senderType(MessageSenderType.CUSTOMER)
                .body("Interested in the studio")
                .build());

        assertThat(message.getBody()).isEqualTo("Interested in the studio");
        assertThat(message.getSenderType()).isEqualTo(MessageSenderType.CUSTOMER);
        assertThat(message.getDirection()).isEqualTo(MessageDirection.INBOUND);
        assertThat(message.getDeliveryStatus()).isEqualTo(MessageDeliveryStatus.DELIVERED);
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.any(MessageReceivedEvent.class));
    }

    @Test
    void shouldIsolateTenantOnGet() {
        UUID conversationId = UUID.randomUUID();
        when(threadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, conversationId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> conversationService.get(conversationId))
                .hasMessageContaining("Conversation not found");
    }
}
