package com.aisales.lead.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.client.AiServiceClient;
import com.aisales.common.contracts.lead.AiLeadQualificationResultDto;
import com.aisales.common.contracts.lead.LeadDto;
import com.aisales.common.contracts.lead.LeadQualityScoreDto;
import com.aisales.common.contracts.lead.LeadStatus;
import com.aisales.common.contracts.lead.QualifyLeadRequest;
import com.aisales.common.contracts.lead.QualifyLeadWithAiRequest;
import com.aisales.common.contracts.lead.RecordLeadQualityScoreRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.util.TenantContext;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LeadAiQualificationServiceTest {

    @Mock private LeadRepository leadRepository;
    @Mock private IndustryQualificationConfigResolver qualificationConfigResolver;
    @Mock private AiServiceClient aiServiceClient;
    @Mock private LeadService leadService;
    @Mock private LeadExtensionService extensionService;

    private LeadAiQualificationService service;
    private UUID tenantId;
    private UUID leadId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        leadId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        service = new LeadAiQualificationService(
                leadRepository,
                new LeadQualificationVariableMapper(),
                qualificationConfigResolver,
                aiServiceClient,
                leadService,
                extensionService,
                new LeadMapper());
        lenient()
                .when(qualificationConfigResolver.resolve(any()))
                .thenAnswer(invocation -> {
                    QualifyLeadWithAiRequest req = invocation.getArgument(0);
                    return new IndustryQualificationConfigResolver.Resolved(
                            req.getPromptCode(), req.getVariableKeys());
                });
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldQualifyRealEstateLeadWhenAiRecommendsQualify() {
        Lead lead = reLead();
        when(leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, leadId))
                .thenReturn(Optional.of(lead));
        when(aiServiceClient.execute(any())).thenReturn(ApiResponse.ok(
                "ok",
                AiExecuteResponse.builder()
                        .executionId(UUID.randomUUID())
                        .promptCode("LEAD_QUALIFY_REAL_ESTATE")
                        .promptVersion(1)
                        .provider("STUB")
                        .renderedUserPrompt("Qualify RE...")
                        .structuredOutput(Map.of("recommendation", "QUALIFY", "score", 82))
                        .confidence(0.9)
                        .build()));
        when(extensionService.recordQualityScore(eq(leadId), any()))
                .thenReturn(LeadQualityScoreDto.builder().overallScore(82).build());
        when(leadService.qualifyLead(eq(leadId), any())).thenReturn(LeadDto.builder()
                .id(leadId)
                .status(LeadStatus.QUALIFIED)
                .qualified(true)
                .build());

        AiLeadQualificationResultDto result = service.qualifyWithAi(leadId, QualifyLeadWithAiRequest.builder()
                .promptCode("LEAD_QUALIFY_REAL_ESTATE")
                .variableKeys(List.of("budget", "location", "timeline"))
                .build());

        assertThat(result.isQualified()).isTrue();
        assertThat(result.getRecommendation()).isEqualTo("QUALIFY");
        assertThat(result.getVariablesUsed())
                .containsEntry("budget", "7500000")
                .containsEntry("location", "Whitefield")
                .containsEntry("timeline", "3 months");

        ArgumentCaptor<AiExecuteRequest> aiCaptor = ArgumentCaptor.forClass(AiExecuteRequest.class);
        verify(aiServiceClient).execute(aiCaptor.capture());
        assertThat(aiCaptor.getValue().getPromptCode()).isEqualTo("LEAD_QUALIFY_REAL_ESTATE");
        verify(leadService).qualifyLead(eq(leadId), any(QualifyLeadRequest.class));
        verify(extensionService).recordQualityScore(eq(leadId), any(RecordLeadQualityScoreRequest.class));
    }

    @Test
    void shouldQualifyAutomobileLeadWhenAiRecommendsQualify() {
        Lead lead = autoLead();
        when(leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, leadId))
                .thenReturn(Optional.of(lead));
        when(aiServiceClient.execute(any())).thenReturn(ApiResponse.ok(
                "ok",
                AiExecuteResponse.builder()
                        .executionId(UUID.randomUUID())
                        .promptCode("LEAD_QUALIFY_AUTOMOBILE")
                        .promptVersion(1)
                        .provider("STUB")
                        .renderedUserPrompt("Qualify Auto...")
                        .structuredOutput(Map.of("recommendation", "QUALIFY", "score", 78))
                        .confidence(0.88)
                        .build()));
        when(extensionService.recordQualityScore(eq(leadId), any()))
                .thenReturn(LeadQualityScoreDto.builder().overallScore(78).build());
        when(leadService.qualifyLead(eq(leadId), any())).thenReturn(LeadDto.builder()
                .id(leadId)
                .status(LeadStatus.QUALIFIED)
                .qualified(true)
                .build());

        AiLeadQualificationResultDto result = service.qualifyWithAi(leadId, QualifyLeadWithAiRequest.builder()
                .promptCode("LEAD_QUALIFY_AUTOMOBILE")
                .variableKeys(List.of("budget", "vehicle", "financeRequired", "exchange"))
                .build());

        assertThat(result.isQualified()).isTrue();
        assertThat(result.getPromptCode()).isEqualTo("LEAD_QUALIFY_AUTOMOBILE");
        assertThat(result.getVariablesUsed())
                .containsEntry("vehicle", "SUV")
                .containsEntry("financeRequired", "true")
                .containsEntry("exchange", "yes");
    }

    @Test
    void shouldNotAutoQualifyWhenAiRecommendsReview() {
        Lead lead = reLead();
        when(leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, leadId))
                .thenReturn(Optional.of(lead));
        when(aiServiceClient.execute(any())).thenReturn(ApiResponse.ok(
                "ok",
                AiExecuteResponse.builder()
                        .executionId(UUID.randomUUID())
                        .promptCode("LEAD_QUALIFY_REAL_ESTATE")
                        .structuredOutput(Map.of("recommendation", "REVIEW"))
                        .confidence(0.85)
                        .build()));
        when(extensionService.recordQualityScore(eq(leadId), any()))
                .thenReturn(LeadQualityScoreDto.builder().overallScore(85).build());

        AiLeadQualificationResultDto result = service.qualifyWithAi(leadId, QualifyLeadWithAiRequest.builder()
                .promptCode("LEAD_QUALIFY_REAL_ESTATE")
                .variableKeys(List.of("budget", "location", "timeline"))
                .build());

        assertThat(result.isQualified()).isFalse();
        assertThat(result.getRecommendation()).isEqualTo("REVIEW");
        verify(extensionService).recordQualityScore(eq(leadId), any());
        org.mockito.Mockito.verify(leadService, org.mockito.Mockito.never()).qualifyLead(any(), any());
    }

    private Lead reLead() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("budget", 7500000);
        attrs.put("location", "Whitefield");
        attrs.put("timeline", "3 months");
        return Lead.builder()
                .id(leadId)
                .tenantId(tenantId)
                .customerName("RE Buyer")
                .phone("+919111111111")
                .sourceType("WEB")
                .status(LeadStatus.NEW)
                .attributes(attrs)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    private Lead autoLead() {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("budget", 1800000);
        attrs.put("vehicle", "SUV");
        attrs.put("financeRequired", true);
        attrs.put("exchange", "yes");
        return Lead.builder()
                .id(leadId)
                .tenantId(tenantId)
                .customerName("Auto Buyer")
                .phone("+919222222222")
                .sourceType("WEB")
                .status(LeadStatus.NEW)
                .attributes(attrs)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
