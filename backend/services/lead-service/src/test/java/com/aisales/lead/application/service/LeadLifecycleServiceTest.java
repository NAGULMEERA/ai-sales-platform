package com.aisales.lead.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.lead.AssignLeadRequest;
import com.aisales.common.contracts.lead.ChangeLeadStatusRequest;
import com.aisales.common.contracts.lead.ConvertLeadRequest;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.contracts.lead.LoseLeadRequest;
import com.aisales.common.contracts.lead.QualifyLeadRequest;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.entity.Lead;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeadLifecycleServiceTest {

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

    private LeadService leadService;
    private UUID tenantId;
    private UUID leadId;
    private Lead lead;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        leadId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());

        lead = Lead.builder()
                .id(leadId)
                .tenantId(tenantId)
                .customerName("Grace Hopper")
                .phone("+911111111111")
                .email("grace@example.com")
                .sourceType("WEB")
                .status(LeadStatus.NEW)
                .validated(false)
                .qualified(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        LeadMapper mapper = new LeadMapper();
        LeadStateMachine stateMachine = new LeadStateMachine();
        LeadSideEffectRecorder sideEffects =
                new LeadSideEffectRecorder(statusHistoryRepository, activityRepository);
        leadService = new LeadService(
                leadRepository, assignmentRepository, noteRepository, activityRepository,
                followupRepository, scoreRepository, statusHistoryRepository, duplicateRepository,
                mapper, eventPublisher, stateMachine, sideEffects, duplicateDetection,
                assignmentPoolService);

        when(leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, leadId))
                .thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldRejectAssignBeforeValidate() {
        assertThatThrownBy(() -> leadService.assignLead(leadId, AssignLeadRequest.builder()
                .assignedTo(UUID.randomUUID())
                .build()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("validated");
    }

    @Test
    void shouldRunHappyPathToWon() {
        when(assignmentRepository.findFirstByLeadIdAndUnassignedAtIsNullOrderByAssignedAtDesc(leadId))
                .thenReturn(Optional.empty());
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(scoreRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        leadService.validateLead(leadId);
        assertThat(lead.isValidated()).isTrue();

        UUID assignee = UUID.randomUUID();
        leadService.assignLead(leadId, AssignLeadRequest.builder().assignedTo(assignee).build());
        assertThat(lead.getAssignedTo()).isEqualTo(assignee);

        leadService.contactLead(leadId, "WHATSAPP");
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.CONTACTED);

        leadService.qualifyLead(leadId, QualifyLeadRequest.builder().score(85).build());
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.QUALIFIED);
        assertThat(lead.isQualified()).isTrue();

        leadService.changeStatus(leadId, ChangeLeadStatusRequest.builder()
                .status(LeadStatus.APPOINTMENT_BOOKED).build());
        leadService.changeStatus(leadId, ChangeLeadStatusRequest.builder()
                .status(LeadStatus.VISITED).build());

        UUID customerId = UUID.randomUUID();
        LeadDto won = leadService.convertLead(leadId, ConvertLeadRequest.builder()
                .customerId(customerId).build());
        assertThat(won.getStatus()).isEqualTo(LeadStatus.WON);
        assertThat(won.getCustomerId()).isEqualTo(customerId);
    }

    @Test
    void shouldRejectTransitionFromWon() {
        lead.setStatus(LeadStatus.WON);
        assertThatThrownBy(() -> leadService.loseLead(leadId, LoseLeadRequest.builder()
                .reason("too late").build()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid lead status transition");
    }

    @Test
    void shouldSoftDelete() {
        leadService.deleteLead(leadId);
        assertThat(lead.getDeletedAt()).isNotNull();
    }
}
