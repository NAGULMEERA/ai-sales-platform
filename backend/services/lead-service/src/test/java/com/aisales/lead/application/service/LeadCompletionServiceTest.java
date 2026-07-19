package com.aisales.lead.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.lead.AddLeadTagRequest;
import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.contracts.lead.ReopenLeadRequest;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.domain.entity.LeadDuplicate;
import com.aisales.lead.domain.entity.LeadTag;
import com.aisales.lead.domain.service.LeadStateMachine;
import com.aisales.lead.infrastructure.persistence.LeadActivityRepository;
import com.aisales.lead.infrastructure.persistence.LeadAssignmentRepository;
import com.aisales.lead.infrastructure.persistence.LeadDuplicateRepository;
import com.aisales.lead.infrastructure.persistence.LeadFollowupRepository;
import com.aisales.lead.infrastructure.persistence.LeadNoteRepository;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import com.aisales.lead.infrastructure.persistence.LeadScoreRepository;
import com.aisales.lead.infrastructure.persistence.LeadStatusHistoryRepository;
import com.aisales.lead.infrastructure.persistence.LeadTagMappingRepository;
import com.aisales.lead.infrastructure.persistence.LeadTagRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
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
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeadCompletionServiceTest {

    @Mock private LeadRepository leadRepository;
    @Mock private LeadAssignmentRepository assignmentRepository;
    @Mock private LeadNoteRepository noteRepository;
    @Mock private LeadActivityRepository activityRepository;
    @Mock private LeadFollowupRepository followupRepository;
    @Mock private LeadScoreRepository scoreRepository;
    @Mock private LeadStatusHistoryRepository statusHistoryRepository;
    @Mock private LeadDuplicateRepository duplicateRepository;
    @Mock private LeadTagRepository tagRepository;
    @Mock private LeadTagMappingRepository mappingRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private DuplicateLeadDetectionService duplicateDetection;
    @Mock private LeadAssignmentPoolService assignmentPoolService;
    @Mock private LeadCustomerConversionGateway customerConversionGateway;
    @Mock private ObjectProvider<?> unusedProvider;

    private LeadService leadService;
    private LeadTagService tagService;
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
                .customerName("Survivor")
                .phone("+911111111111")
                .email("survivor@example.com")
                .sourceType("WEB")
                .status(LeadStatus.LOST)
                .validated(true)
                .qualified(true)
                .assignedTo(UUID.randomUUID())
                .attributes(new HashMap<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();

        LeadMapper mapper = new LeadMapper();
        LeadSideEffectRecorder sideEffects =
                new LeadSideEffectRecorder(statusHistoryRepository, activityRepository);
        leadService = new LeadService(
                leadRepository,
                assignmentRepository,
                noteRepository,
                activityRepository,
                followupRepository,
                scoreRepository,
                statusHistoryRepository,
                duplicateRepository,
                mapper,
                eventPublisher,
                new LeadStateMachine(),
                sideEffects,
                duplicateDetection,
                assignmentPoolService,
                org.mockito.Mockito.mock(PipelineService.class),
                customerConversionGateway,
                noopTxManager(),
                org.mockito.Mockito.mock(LeadIdempotencyService.class),
                (ObjectProvider) unusedProvider,
                (ObjectProvider) unusedProvider);
        tagService = new LeadTagService(leadRepository, tagRepository, mappingRepository, sideEffects);

        when(leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, leadId))
                .thenReturn(Optional.of(lead));
        when(leadRepository.findByTenantIdAndId(tenantId, leadId)).thenReturn(Optional.of(lead));
        when(leadRepository.save(any(Lead.class))).thenAnswer(inv -> inv.getArgument(0));
        when(statusHistoryRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(activityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(unusedProvider.getIfAvailable()).thenReturn(null);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldUnassignLead() {
        lead.setStatus(LeadStatus.QUALIFIED);
        UUID previous = lead.getAssignedTo();
        when(assignmentRepository.findFirstByLeadIdAndUnassignedAtIsNullOrderByAssignedAtDesc(leadId))
                .thenReturn(Optional.empty());

        var dto = leadService.unassignLead(leadId, "released");
        assertThat(dto.getAssignedTo()).isNull();
        assertThat(lead.getAssignedTo()).isNull();
        assertThat(previous).isNotNull();
    }

    @Test
    void shouldReopenLostLead() {
        var dto = leadService.reopenLead(
                leadId, ReopenLeadRequest.builder().status(LeadStatus.QUALIFIED).reason("new interest").build());
        assertThat(dto.getStatus()).isEqualTo(LeadStatus.QUALIFIED);
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.QUALIFIED);
    }

    @Test
    void shouldRejectReopenWhenNotLost() {
        lead.setStatus(LeadStatus.QUALIFIED);
        assertThatThrownBy(() -> leadService.reopenLead(leadId, new ReopenLeadRequest()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("LOST");
    }

    @Test
    void shouldRestoreSoftDeletedLead() {
        lead.setDeletedAt(Instant.now());
        var dto = leadService.restoreLead(leadId);
        assertThat(dto).isNotNull();
        assertThat(lead.getDeletedAt()).isNull();
    }

    @Test
    void shouldMergeDuplicateIntoSurvivor() {
        UUID loserId = UUID.randomUUID();
        Lead loser = Lead.builder()
                .id(loserId)
                .tenantId(tenantId)
                .customerName("Loser")
                .phone("+922222222222")
                .email("loser@example.com")
                .sourceType("WEB")
                .status(LeadStatus.NEW)
                .attributes(new HashMap<>(java.util.Map.of("budget", "1Cr")))
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .version(0L)
                .build();
        lead.setStatus(LeadStatus.QUALIFIED);
        lead.setEmail(null);

        UUID duplicateId = UUID.randomUUID();
        LeadDuplicate dup = LeadDuplicate.builder()
                .id(duplicateId)
                .tenantId(tenantId)
                .leadId(leadId)
                .duplicateOfLeadId(loserId)
                .similarityScore(BigDecimal.ONE)
                .detectedAt(Instant.now())
                .resolved(false)
                .build();

        when(leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, loserId))
                .thenReturn(Optional.of(loser));
        when(duplicateRepository.findByTenantIdAndId(tenantId, duplicateId)).thenReturn(Optional.of(dup));
        when(duplicateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var result = leadService.resolveDuplicate(leadId, duplicateId, leadId, true);
        assertThat(result.isResolved()).isTrue();
        assertThat(lead.getEmail()).isEqualTo("loser@example.com");
        assertThat(lead.getAttributes()).containsEntry("budget", "1Cr");
        assertThat(loser.getDeletedAt()).isNotNull();
    }

    @Test
    void shouldAddTagToLead() {
        when(tagRepository.findByTenantIdAndTagIgnoreCase(tenantId, "hot"))
                .thenReturn(Optional.empty());
        when(tagRepository.save(any(LeadTag.class))).thenAnswer(inv -> {
            LeadTag tag = inv.getArgument(0);
            tag.setId(UUID.randomUUID());
            return tag;
        });
        when(mappingRepository.existsByLeadIdAndTagId(any(), any())).thenReturn(false);
        when(mappingRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = tagService.addTag(leadId, AddLeadTagRequest.builder().tag("hot").color("#f00").build());
        assertThat(dto.getTag()).isEqualTo("hot");
    }

    private static PlatformTransactionManager noopTxManager() {
        return new AbstractPlatformTransactionManager() {
            @Override
            protected Object doGetTransaction() {
                return new Object();
            }

            @Override
            protected void doBegin(Object transaction, TransactionDefinition definition) {}

            @Override
            protected void doCommit(DefaultTransactionStatus status) {}

            @Override
            protected void doRollback(DefaultTransactionStatus status) {}
        };
    }
}
