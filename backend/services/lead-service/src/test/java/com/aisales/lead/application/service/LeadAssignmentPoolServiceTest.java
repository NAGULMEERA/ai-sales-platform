package com.aisales.lead.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.lead.AssignLeadRequest;
import com.aisales.common.contracts.lead.AssignmentStrategy;
import com.aisales.common.contracts.lead.UpsertAssignmentPoolMemberRequest;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.domain.entity.LeadAssignmentPoolMember;
import com.aisales.lead.domain.service.LeadStateMachine;
import com.aisales.lead.infrastructure.persistence.LeadActivityRepository;
import com.aisales.lead.infrastructure.persistence.LeadAssignmentPoolRepository;
import com.aisales.lead.infrastructure.persistence.LeadAssignmentRepository;
import com.aisales.lead.infrastructure.persistence.LeadDuplicateRepository;
import com.aisales.lead.infrastructure.persistence.LeadFollowupRepository;
import com.aisales.lead.infrastructure.persistence.LeadNoteRepository;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import com.aisales.lead.infrastructure.persistence.LeadScoreRepository;
import com.aisales.lead.infrastructure.persistence.LeadStatusHistoryRepository;
import com.aisales.common.contracts.lead.LeadStatus;
import java.time.Instant;
import java.util.List;
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
class LeadAssignmentPoolServiceTest {

    @Mock private LeadAssignmentPoolRepository poolRepository;
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

    private LeadAssignmentPoolService poolService;
    private LeadService leadService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        LeadMapper mapper = new LeadMapper();
        poolService = new LeadAssignmentPoolService(poolRepository, mapper);
        LeadSideEffectRecorder sideEffects =
                new LeadSideEffectRecorder(statusHistoryRepository, activityRepository);
        leadService = new LeadService(
                leadRepository, assignmentRepository, noteRepository, activityRepository,
                followupRepository, scoreRepository, statusHistoryRepository, duplicateRepository,
                mapper, eventPublisher, new LeadStateMachine(), sideEffects, duplicateDetection,
                poolService, org.mockito.Mockito.mock(PipelineService.class),
                org.mockito.Mockito.mock(LeadCustomerConversionGateway.class),
                noopTxManager(), org.mockito.Mockito.mock(LeadIdempotencyService.class));
    }

    private static org.springframework.transaction.PlatformTransactionManager noopTxManager() {
        return new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object transaction,
                    org.springframework.transaction.TransactionDefinition definition) {}
            @Override protected void doCommit(
                    org.springframework.transaction.support.DefaultTransactionStatus status) {}
            @Override protected void doRollback(
                    org.springframework.transaction.support.DefaultTransactionStatus status) {}
        };
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldUpsertPoolMember() {
        UUID userId = UUID.randomUUID();
        when(poolRepository.findByTenantIdAndUserId(tenantId, userId)).thenReturn(Optional.empty());
        when(poolRepository.save(any())).thenAnswer(inv -> {
            LeadAssignmentPoolMember m = inv.getArgument(0);
            m.setId(UUID.randomUUID());
            return m;
        });

        var dto = poolService.upsert(UpsertAssignmentPoolMemberRequest.builder()
                .userId(userId)
                .enabled(true)
                .build());

        assertThat(dto.getUserId()).isEqualTo(userId);
        assertThat(dto.isEnabled()).isTrue();
        verify(poolRepository).save(any());
    }

    @Test
    void shouldPickLeastRecentlyAssignedForRoundRobin() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        when(poolRepository.findEnabledForRoundRobin(tenantId)).thenReturn(List.of(
                LeadAssignmentPoolMember.builder()
                        .tenantId(tenantId).userId(first).enabled(true).build(),
                LeadAssignmentPoolMember.builder()
                        .tenantId(tenantId).userId(second).enabled(true)
                        .lastAssignedAt(Instant.now()).build()));
        when(poolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(poolService.nextRoundRobinAssignee(tenantId)).isEqualTo(first);
    }

    @Test
    void shouldRejectRoundRobinWhenPoolEmpty() {
        when(poolRepository.findEnabledForRoundRobin(tenantId)).thenReturn(List.of());
        assertThatThrownBy(() -> poolService.nextRoundRobinAssignee(tenantId))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("round-robin");
    }

    @Test
    void shouldAssignViaRoundRobinStrategy() {
        UUID leadId = UUID.randomUUID();
        UUID assignee = UUID.randomUUID();
        Lead lead = Lead.builder()
                .id(leadId)
                .tenantId(tenantId)
                .customerName("RR Lead")
                .phone("+910000000001")
                .sourceType("WEB")
                .status(LeadStatus.NEW)
                .validated(true)
                .qualified(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        when(leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, leadId))
                .thenReturn(Optional.of(lead));
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(assignmentRepository.findFirstByLeadIdAndUnassignedAtIsNullOrderByAssignedAtDesc(leadId))
                .thenReturn(Optional.empty());
        when(assignmentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(poolRepository.findEnabledForRoundRobin(tenantId)).thenReturn(List.of(
                LeadAssignmentPoolMember.builder()
                        .tenantId(tenantId).userId(assignee).enabled(true).build()));
        when(poolRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = leadService.assignLead(leadId, AssignLeadRequest.builder()
                .strategy(AssignmentStrategy.ROUND_ROBIN)
                .build());

        assertThat(dto.getAssignedTo()).isEqualTo(assignee);
    }
}
