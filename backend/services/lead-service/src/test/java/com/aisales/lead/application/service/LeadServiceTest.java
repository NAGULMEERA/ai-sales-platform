package com.aisales.lead.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.lead.CreateLeadRequest;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.LeadCreatedEvent;
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
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadServiceTest {

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
    private UUID tenantId;
    private UUID pipelineId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        pipelineId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        LeadMapper mapper = new LeadMapper();
        LeadStateMachine stateMachine = new LeadStateMachine();
        LeadSideEffectRecorder sideEffects =
                new LeadSideEffectRecorder(statusHistoryRepository, activityRepository);
        lenient().when(pipelineService.resolvePipelineIdForCreate(any(UUID.class), any())).thenReturn(pipelineId);
        leadService = new LeadService(
                leadRepository, assignmentRepository, noteRepository, activityRepository,
                followupRepository, scoreRepository, statusHistoryRepository, duplicateRepository,
                mapper, eventPublisher, stateMachine, sideEffects, duplicateDetection,
                assignmentPoolService, pipelineService, customerConversionGateway);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateLeadAndPublishLeadCreated() {
        CreateLeadRequest request = CreateLeadRequest.builder()
                .customerName("Ada Lovelace")
                .phone("+919876543210")
                .email("ada@example.com")
                .sourceType("WHATSAPP")
                .campaign("spring-launch")
                .build();

        when(leadRepository.saveAndFlush(any(Lead.class))).thenAnswer(invocation -> {
            Lead lead = invocation.getArgument(0);
            lead.setId(UUID.randomUUID());
            lead.setExternalId("LD-20260718-100001");
            return lead;
        });

        LeadDto dto = leadService.createLead(request);

        assertThat(dto.getCustomerName()).isEqualTo("Ada Lovelace");
        assertThat(dto.getStatus()).isEqualTo(LeadStatus.NEW);
        assertThat(dto.isValidated()).isFalse();
        assertThat(dto.getTenantId()).isEqualTo(tenantId);
        assertThat(dto.getPipelineId()).isEqualTo(pipelineId);

        ArgumentCaptor<LeadCreatedEvent> captor = ArgumentCaptor.forClass(LeadCreatedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("LeadCreated");
        verify(duplicateDetection).detectAndRecord(any(Lead.class));
    }

    @Test
    void shouldRequireTenantContext() {
        TenantContext.clear();
        CreateLeadRequest request = CreateLeadRequest.builder()
                .customerName("No Tenant")
                .phone("+913333333333")
                .sourceType("WEB")
                .build();

        assertThatThrownBy(() -> leadService.createLead(request))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Tenant context");
    }
}
