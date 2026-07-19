package com.aisales.lead.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.service.LeadStateMachine;
import com.aisales.lead.infrastructure.persistence.LeadActivityRepository;
import com.aisales.lead.infrastructure.persistence.LeadAssignmentRepository;
import com.aisales.lead.infrastructure.persistence.LeadDuplicateRepository;
import com.aisales.lead.infrastructure.persistence.LeadFollowupRepository;
import com.aisales.lead.infrastructure.persistence.LeadNoteRepository;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import com.aisales.lead.infrastructure.persistence.LeadScoreRepository;
import com.aisales.lead.infrastructure.persistence.LeadStatusHistoryRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadTenantIsolationTest {

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
    private UUID tenantA;
    private UUID leadOwnedByTenantB;

    @BeforeEach
    void setUp() {
        tenantA = UUID.randomUUID();
        leadOwnedByTenantB = UUID.randomUUID();
        TenantContext.setTenantId(tenantA.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        LeadSideEffectRecorder sideEffects =
                new LeadSideEffectRecorder(statusHistoryRepository, activityRepository);
        leadService = new LeadService(
                leadRepository, assignmentRepository, noteRepository, activityRepository,
                followupRepository, scoreRepository, statusHistoryRepository, duplicateRepository,
                new LeadMapper(), eventPublisher, new LeadStateMachine(), sideEffects,
                duplicateDetection, assignmentPoolService,
                org.mockito.Mockito.mock(PipelineService.class),
                org.mockito.Mockito.mock(LeadCustomerConversionGateway.class),
                noopTxManager(), org.mockito.Mockito.mock(LeadIdempotencyService.class), org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class), org.mockito.Mockito.mock(org.springframework.beans.factory.ObjectProvider.class));
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
    void shouldNotAllowTenantAToReadTenantBLead() {
        when(leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantA, leadOwnedByTenantB))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> leadService.getLead(leadOwnedByTenantB))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining(leadOwnedByTenantB.toString());
    }
}
