package com.aisales.lead.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.lead.ArchiveLeadRequest;
import com.aisales.common.contracts.lead.CancelLeadVisitRequest;
import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.contracts.lead.ScheduleLeadVisitRequest;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.BaseEvent;
import com.aisales.common.events.model.LeadVisitScheduledEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.domain.service.LeadStateMachine;
import com.aisales.lead.infrastructure.persistence.LeadActivityRepository;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeadJourneyServiceTest {

    @Mock private LeadRepository leadRepository;
    @Mock private LeadActivityRepository activityRepository;
    @Mock private LeadStatusHistoryRepository statusHistoryRepository;
    @Mock private EventPublisher eventPublisher;

    private LeadJourneyService journeyService;
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
                .customerName("Journey Lead")
                .phone("+910000000099")
                .sourceType("WEB")
                .status(LeadStatus.QUALIFIED)
                .validated(true)
                .qualified(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();
        LeadSideEffectRecorder sideEffects =
                new LeadSideEffectRecorder(statusHistoryRepository, activityRepository);
        journeyService = new LeadJourneyService(
                leadRepository, activityRepository, new LeadMapper(), eventPublisher,
                new LeadStateMachine(), sideEffects);
        when(leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, leadId))
                .thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldScheduleCompleteAndArchiveVisitJourney() {
        journeyService.scheduleVisit(leadId, ScheduleLeadVisitRequest.builder()
                .scheduledAt(Instant.parse("2026-07-21T09:00:00Z"))
                .location("Skyline Tower")
                .build());
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.APPOINTMENT_BOOKED);

        ArgumentCaptor<BaseEvent> events = ArgumentCaptor.forClass(BaseEvent.class);
        verify(eventPublisher, org.mockito.Mockito.atLeastOnce()).publish(events.capture());
        assertThat(events.getAllValues()).anyMatch(LeadVisitScheduledEvent.class::isInstance);

        journeyService.completeVisit(leadId, "Customer liked unit 1204");
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.VISITED);

        lead.setStatus(LeadStatus.WON);
        journeyService.archiveLead(leadId, ArchiveLeadRequest.builder().reason("retention").build());
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.ARCHIVED);
    }

    @Test
    void shouldCancelScheduledVisitBackToQualified() {
        lead.setStatus(LeadStatus.APPOINTMENT_BOOKED);
        journeyService.cancelVisit(leadId, CancelLeadVisitRequest.builder().reason("rain").build());
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.QUALIFIED);
    }

    @Test
    void shouldRejectArchiveFromOpenLead() {
        assertThatThrownBy(() -> journeyService.archiveLead(leadId, new ArchiveLeadRequest()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid lead status transition");
    }
}
