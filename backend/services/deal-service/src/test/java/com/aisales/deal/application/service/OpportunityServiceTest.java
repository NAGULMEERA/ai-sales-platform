package com.aisales.deal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.deal.AssignOpportunityRequest;
import com.aisales.common.contracts.deal.CreateOpportunityRequest;
import com.aisales.common.contracts.deal.OpportunityDto;
import com.aisales.common.contracts.deal.OpportunityStatus;
import com.aisales.common.contracts.deal.UpdateOpportunityRequest;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.OpportunityAssignedEvent;
import com.aisales.common.events.model.OpportunityCreatedEvent;
import com.aisales.common.events.model.OpportunityStatusChangedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.deal.application.mapper.DealMapper;
import com.aisales.deal.domain.entity.Opportunity;
import com.aisales.deal.infrastructure.persistence.OpportunityRepository;
import com.aisales.deal.infrastructure.persistence.OpportunityTimelineRepository;
import java.math.BigDecimal;
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
class OpportunityServiceTest {

    @Mock private OpportunityRepository opportunityRepository;
    @Mock private OpportunityTimelineRepository timelineRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private ObjectProvider<?> platformMetrics;

    private OpportunityService opportunityService;
    private UUID tenantId;
    private UUID actorId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        actorId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(actorId.toString());
        org.mockito.Mockito.lenient().when(platformMetrics.getIfAvailable()).thenReturn(null);
        org.mockito.Mockito.lenient()
                .when(timelineRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenAnswer(inv -> inv.getArgument(0));
        opportunityService = new OpportunityService(
                opportunityRepository,
                timelineRepository,
                new OpportunityTimelineRecorder(timelineRepository),
                new DealMapper(),
                eventPublisher,
                (ObjectProvider) platformMetrics);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateOpportunityAndPublishEvent() {
        when(opportunityRepository.saveAndFlush(any(Opportunity.class))).thenAnswer(inv -> {
            Opportunity o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });

        UUID customerId = UUID.randomUUID();
        OpportunityDto dto = opportunityService.create(CreateOpportunityRequest.builder()
                .customerId(customerId)
                .name("Studio opportunity")
                .amount(new BigDecimal("1000000"))
                .currency("INR")
                .build());

        assertThat(dto.getName()).isEqualTo("Studio opportunity");
        assertThat(dto.getStatus()).isEqualTo(OpportunityStatus.OPEN);
        assertThat(dto.getAssignedTo()).isEqualTo(actorId);
        assertThat(dto.getTenantId()).isEqualTo(tenantId);

        ArgumentCaptor<OpportunityCreatedEvent> captor =
                ArgumentCaptor.forClass(OpportunityCreatedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("OpportunityCreated");
    }

    @Test
    void shouldAssignOwner() {
        UUID opportunityId = UUID.randomUUID();
        UUID previousOwner = UUID.randomUUID();
        UUID newOwner = UUID.randomUUID();
        Opportunity opportunity = Opportunity.builder()
                .id(opportunityId)
                .tenantId(tenantId)
                .customerId(UUID.randomUUID())
                .name("Deal")
                .currency("INR")
                .status(OpportunityStatus.OPEN)
                .assignedTo(previousOwner)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        when(opportunityRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, opportunityId))
                .thenReturn(Optional.of(opportunity));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> inv.getArgument(0));

        OpportunityDto dto = opportunityService.assign(
                opportunityId, AssignOpportunityRequest.builder().assignedTo(newOwner).build());

        assertThat(dto.getAssignedTo()).isEqualTo(newOwner);
        ArgumentCaptor<OpportunityAssignedEvent> captor =
                ArgumentCaptor.forClass(OpportunityAssignedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getAssignedTo()).isEqualTo(newOwner.toString());
        assertThat(captor.getValue().getPreviousAssignedTo()).isEqualTo(previousOwner.toString());
    }

    @Test
    void shouldPublishStatusChange() {
        UUID opportunityId = UUID.randomUUID();
        Opportunity opportunity = Opportunity.builder()
                .id(opportunityId)
                .tenantId(tenantId)
                .customerId(UUID.randomUUID())
                .name("Deal")
                .currency("INR")
                .status(OpportunityStatus.OPEN)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        when(opportunityRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, opportunityId))
                .thenReturn(Optional.of(opportunity));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> inv.getArgument(0));

        opportunityService.update(opportunityId, UpdateOpportunityRequest.builder()
                .status(OpportunityStatus.LOST)
                .build());

        ArgumentCaptor<OpportunityStatusChangedEvent> captor =
                ArgumentCaptor.forClass(OpportunityStatusChangedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getPreviousStatus()).isEqualTo("OPEN");
        assertThat(captor.getValue().getStatus()).isEqualTo("LOST");
    }

    @Test
    void shouldCloseWonAndPublishWonEvent() {
        UUID opportunityId = UUID.randomUUID();
        Opportunity opportunity = Opportunity.builder()
                .id(opportunityId)
                .tenantId(tenantId)
                .customerId(UUID.randomUUID())
                .name("Deal")
                .currency("INR")
                .status(OpportunityStatus.NEGOTIATION)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        when(opportunityRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, opportunityId))
                .thenReturn(Optional.of(opportunity));
        when(opportunityRepository.save(any(Opportunity.class))).thenAnswer(inv -> inv.getArgument(0));

        OpportunityDto dto = opportunityService.closeWon(
                opportunityId,
                com.aisales.common.contracts.deal.CloseOpportunityRequest.builder()
                        .reason("contract signed")
                        .build());

        assertThat(dto.getStatus()).isEqualTo(OpportunityStatus.WON);
        verify(eventPublisher).publish(any(com.aisales.common.events.model.OpportunityWonEvent.class));
    }

    @Test
    void shouldRejectAssignOnTerminalOpportunity() {
        UUID opportunityId = UUID.randomUUID();
        Opportunity opportunity = Opportunity.builder()
                .id(opportunityId)
                .tenantId(tenantId)
                .customerId(UUID.randomUUID())
                .name("Closed")
                .currency("INR")
                .status(OpportunityStatus.WON)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        when(opportunityRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, opportunityId))
                .thenReturn(Optional.of(opportunity));

        assertThatThrownBy(() -> opportunityService.assign(
                        opportunityId,
                        AssignOpportunityRequest.builder().assignedTo(UUID.randomUUID()).build()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("terminal");
    }
}
