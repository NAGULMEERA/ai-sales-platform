package com.aisales.lead.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.lead.CreateLeadFollowupRequest;
import com.aisales.common.contracts.lead.LeadFollowupDto;
import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.domain.entity.LeadActivity;
import com.aisales.lead.domain.entity.LeadFollowup;
import com.aisales.lead.domain.service.LeadStateMachine;
import com.aisales.lead.infrastructure.persistence.LeadActivityRepository;
import com.aisales.lead.infrastructure.persistence.LeadAssignmentRepository;
import com.aisales.lead.infrastructure.persistence.LeadDuplicateRepository;
import com.aisales.lead.infrastructure.persistence.LeadFollowupRepository;
import com.aisales.lead.infrastructure.persistence.LeadNoteRepository;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import com.aisales.lead.infrastructure.persistence.LeadScoreRepository;
import com.aisales.lead.infrastructure.persistence.LeadStatusHistoryRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

/**
 * Sprint 6: same follow-up + timeline APIs; visit vs test-drive differ only by followupType metadata.
 */
@ExtendWith(MockitoExtension.class)
class IndustryConversationFollowupFlowTest {

    @Mock private LeadRepository leadRepository;
    @Mock private LeadAssignmentRepository assignmentRepository;
    @Mock private LeadNoteRepository noteRepository;
    @Mock private LeadActivityRepository activityRepository;
    @Mock private LeadFollowupRepository followupRepository;
    @Mock private LeadScoreRepository scoreRepository;
    @Mock private LeadStatusHistoryRepository statusHistoryRepository;
    @Mock private LeadDuplicateRepository duplicateRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private DuplicateLeadDetectionService duplicateDetection;
    @Mock private LeadAssignmentPoolService assignmentPoolService;
    @Mock private PipelineService pipelineService;
    @Mock private LeadCustomerConversionGateway customerConversionGateway;

    private LeadService leadService;
    private LeadConversationTimelineService timelineService;
    private UUID tenantId;
    private UUID leadId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        leadId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        LeadMapper mapper = new LeadMapper();
        LeadSideEffectRecorder sideEffects =
                new LeadSideEffectRecorder(statusHistoryRepository, activityRepository);
        leadService = new LeadService(
                leadRepository, assignmentRepository, noteRepository, activityRepository,
                followupRepository, scoreRepository, statusHistoryRepository, duplicateRepository,
                mapper, eventPublisher, new LeadStateMachine(), sideEffects, duplicateDetection,
                assignmentPoolService, pipelineService, customerConversionGateway, noopTxManager(), org.mockito.Mockito.mock(LeadIdempotencyService.class), org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class), org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class));
        timelineService = new LeadConversationTimelineService(sideEffects);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldScheduleRealEstateVisitFollowupOnSameApi() {
        stubLead();
        when(followupRepository.save(any(LeadFollowup.class))).thenAnswer(inv -> {
            LeadFollowup f = inv.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });
        when(activityRepository.save(any(LeadActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        LeadFollowupDto dto = leadService.scheduleFollowup(leadId, CreateLeadFollowupRequest.builder()
                .scheduledAt(Instant.parse("2026-07-20T10:00:00Z"))
                .followupType("VISIT_FOLLOWUP")
                .note("Post site-visit check-in")
                .build());

        assertThat(dto.getFollowupType()).isEqualTo("VISIT_FOLLOWUP");
        ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);
        verify(activityRepository).save(activityCaptor.capture());
        assertThat(activityCaptor.getValue().getActivityType()).isEqualTo("FOLLOWUP_SCHEDULED");
        assertThat(activityCaptor.getValue().getDescription()).contains("VISIT_FOLLOWUP");
    }

    @Test
    void shouldScheduleAutomobileTestDriveFollowupOnSameApi() {
        stubLead();
        when(followupRepository.save(any(LeadFollowup.class))).thenAnswer(inv -> {
            LeadFollowup f = inv.getArgument(0);
            f.setId(UUID.randomUUID());
            return f;
        });
        when(activityRepository.save(any(LeadActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        LeadFollowupDto dto = leadService.scheduleFollowup(leadId, CreateLeadFollowupRequest.builder()
                .scheduledAt(Instant.parse("2026-07-21T11:00:00Z"))
                .followupType("TEST_DRIVE_FOLLOWUP")
                .note("Post test-drive check-in")
                .build());

        assertThat(dto.getFollowupType()).isEqualTo("TEST_DRIVE_FOLLOWUP");
        ArgumentCaptor<LeadActivity> activityCaptor = ArgumentCaptor.forClass(LeadActivity.class);
        verify(activityRepository).save(activityCaptor.capture());
        assertThat(activityCaptor.getValue().getActivityType()).isEqualTo("FOLLOWUP_SCHEDULED");
        assertThat(activityCaptor.getValue().getDescription()).contains("TEST_DRIVE_FOLLOWUP");
    }

    @Test
    void shouldProjectConversationOntoLeadTimelineForBothIndustries() {
        String conversationId = UUID.randomUUID().toString();
        when(activityRepository.save(any(LeadActivity.class))).thenAnswer(inv -> inv.getArgument(0));

        timelineService.onConversationStarted(leadId.toString(), conversationId, "WHATSAPP");
        timelineService.onMessageAdded(
                leadId.toString(), conversationId, UUID.randomUUID().toString(), "AGENT");
        timelineService.onConversationClosed(leadId.toString(), conversationId, "resolved");

        ArgumentCaptor<LeadActivity> captor = ArgumentCaptor.forClass(LeadActivity.class);
        verify(activityRepository, org.mockito.Mockito.times(3)).save(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(LeadActivity::getActivityType)
                .containsExactly(
                        "CONVERSATION_STARTED",
                        "CONVERSATION_MESSAGE",
                        "CONVERSATION_CLOSED");
    }

    private void stubLead() {
        when(leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(eq(tenantId), eq(leadId)))
                .thenReturn(Optional.of(Lead.builder()
                        .id(leadId)
                        .tenantId(tenantId)
                        .customerName("Buyer")
                        .phone("+919999999999")
                        .sourceType("WEB")
                        .status(LeadStatus.QUALIFIED)
                        .createdAt(Instant.now())
                        .updatedAt(Instant.now())
                        .build()));
    }

    private static PlatformTransactionManager noopTxManager() {
        return new AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object transaction, TransactionDefinition definition) {}
            @Override protected void doCommit(DefaultTransactionStatus status) {}
            @Override protected void doRollback(DefaultTransactionStatus status) {}
        };
    }
}