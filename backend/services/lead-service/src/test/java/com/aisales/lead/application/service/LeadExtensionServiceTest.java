package com.aisales.lead.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.lead.CreateLeadAttachmentRequest;
import com.aisales.common.contracts.lead.CreateLeadAttributionRequest;
import com.aisales.common.contracts.lead.CreateLeadCustomFieldRequest;
import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.contracts.lead.RecordLeadQualityScoreRequest;
import com.aisales.common.core.util.TenantContext;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.domain.entity.LeadAttachment;
import com.aisales.lead.domain.entity.LeadAttribution;
import com.aisales.lead.domain.entity.LeadCustomFieldDefinition;
import com.aisales.lead.domain.entity.LeadQualityScore;
import com.aisales.lead.infrastructure.persistence.LeadActivityRepository;
import com.aisales.lead.infrastructure.persistence.LeadAttachmentRepository;
import com.aisales.lead.infrastructure.persistence.LeadAttributionRepository;
import com.aisales.lead.infrastructure.persistence.LeadCustomFieldRepository;
import com.aisales.lead.infrastructure.persistence.LeadQualityScoreRepository;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
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
class LeadExtensionServiceTest {

    @Mock private LeadRepository leadRepository;
    @Mock private LeadAttachmentRepository attachmentRepository;
    @Mock private LeadCustomFieldRepository customFieldRepository;
    @Mock private LeadAttributionRepository attributionRepository;
    @Mock private LeadQualityScoreRepository qualityScoreRepository;
    @Mock private LeadStatusHistoryRepository statusHistoryRepository;
    @Mock private LeadActivityRepository activityRepository;

    private LeadExtensionService extensionService;
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
                .customerName("Ext Lead")
                .phone("+910000000002")
                .sourceType("WEB")
                .status(LeadStatus.NEW)
                .validated(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        LeadMapper mapper = new LeadMapper();
        LeadSideEffectRecorder sideEffects =
                new LeadSideEffectRecorder(statusHistoryRepository, activityRepository);
        extensionService = new LeadExtensionService(
                leadRepository, attachmentRepository, customFieldRepository,
                attributionRepository, qualityScoreRepository, mapper, sideEffects);
        when(leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, leadId))
                .thenReturn(Optional.of(lead));
        when(activityRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldAddAttachment() {
        when(attachmentRepository.save(any(LeadAttachment.class))).thenAnswer(inv -> {
            LeadAttachment attachment = inv.getArgument(0);
            attachment.setId(UUID.randomUUID());
            return attachment;
        });

        var dto = extensionService.addAttachment(leadId, CreateLeadAttachmentRequest.builder()
                .fileName("brief.pdf")
                .fileUrl("s3://bucket/brief.pdf")
                .fileType("application/pdf")
                .fileSize(1024L)
                .build());

        assertThat(dto.getFileName()).isEqualTo("brief.pdf");
        assertThat(dto.getLeadId()).isEqualTo(leadId);
    }

    @Test
    void shouldCreateCustomField() {
        when(customFieldRepository.save(any(LeadCustomFieldDefinition.class))).thenAnswer(inv -> {
            LeadCustomFieldDefinition field = inv.getArgument(0);
            field.setId(UUID.randomUUID());
            return field;
        });

        var dto = extensionService.createCustomField(CreateLeadCustomFieldRequest.builder()
                .fieldName("budget_band")
                .fieldType("SELECT")
                .required(true)
                .displayOrder(1)
                .build());

        assertThat(dto.getFieldName()).isEqualTo("budget_band");
        assertThat(dto.getTenantId()).isEqualTo(tenantId);
        assertThat(dto.isRequired()).isTrue();
    }

    @Test
    void shouldAddAttribution() {
        when(attributionRepository.save(any(LeadAttribution.class))).thenAnswer(inv -> {
            LeadAttribution attribution = inv.getArgument(0);
            attribution.setId(UUID.randomUUID());
            return attribution;
        });

        var dto = extensionService.addAttribution(leadId, CreateLeadAttributionRequest.builder()
                .channel("META_ADS")
                .campaign("spring")
                .build());

        assertThat(dto.getChannel()).isEqualTo("META_ADS");
    }

    @Test
    void shouldRecordQualityScoreAndUpdateLead() {
        when(qualityScoreRepository.save(any(LeadQualityScore.class))).thenAnswer(inv -> {
            LeadQualityScore score = inv.getArgument(0);
            score.setId(UUID.randomUUID());
            return score;
        });
        when(leadRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var dto = extensionService.recordQualityScore(leadId, RecordLeadQualityScoreRequest.builder()
                .overallScore(88)
                .nextAction("Schedule site visit")
                .build());

        assertThat(dto.getOverallScore()).isEqualTo(88);
        assertThat(lead.getScore()).isEqualTo(88);
        verify(leadRepository).save(lead);
    }
}
