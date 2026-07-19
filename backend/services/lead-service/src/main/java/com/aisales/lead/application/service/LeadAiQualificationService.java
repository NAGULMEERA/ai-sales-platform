package com.aisales.lead.application.service;

import com.aisales.common.contracts.ai.QualificationResultDto;
import com.aisales.common.contracts.ai.QualifyLeadAiRequest;
import com.aisales.common.contracts.client.AiServiceClient;
import com.aisales.common.contracts.lead.AiLeadQualificationResultDto;
import com.aisales.common.contracts.lead.QualifyLeadRequest;
import com.aisales.common.contracts.lead.QualifyLeadWithAiRequest;
import com.aisales.common.contracts.lead.RecordLeadQualityScoreRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.lead.application.mapper.LeadMapper;
import com.aisales.lead.domain.entity.Lead;
import com.aisales.lead.infrastructure.persistence.LeadRepository;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Coordinates AI qualification. Assembles lead context and calls AI Gateway qualify orchestration.
 * AI recommends; Lead aggregate decides.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadAiQualificationService {

    private static final Set<String> QUALIFY_RECOMMENDATIONS =
            Set.of("QUALIFY", "QUALIFIED", "APPROVE", "APPROVED");

    private final LeadRepository leadRepository;
    private final LeadQualificationVariableMapper variableMapper;
    private final IndustryQualificationConfigResolver qualificationConfigResolver;
    private final AiServiceClient aiServiceClient;
    private final LeadService leadService;
    private final LeadExtensionService extensionService;
    private final LeadMapper leadMapper;

    public AiLeadQualificationResultDto qualifyWithAi(UUID leadId, QualifyLeadWithAiRequest request) {
        Lead lead = leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found: " + leadId));

        IndustryQualificationConfigResolver.Resolved config = qualificationConfigResolver.resolve(request);

        Map<String, String> variables = variableMapper.toVariables(
                lead.getCustomerName(), lead.getAttributes(), config.variableKeys());
        putContext(variables, "leadId", leadId.toString());
        putContext(variables, "sourceType", lead.getSourceType());
        putContext(variables, "status", lead.getStatus() != null ? lead.getStatus().name() : null);
        putContext(variables, "phone", lead.getPhone());
        putContext(variables, "email", lead.getEmail());
        if (lead.getCustomerId() != null) {
            putContext(variables, "customerId", lead.getCustomerId().toString());
        }
        if (lead.getScore() != null) {
            putContext(variables, "currentScore", String.valueOf(lead.getScore()));
        }

        QualificationResultDto qualification = executeQualify(config, variables, leadId);

        String recommendation = qualification.getRecommendation();
        Integer suggestedScore = qualification.getQualificationScore() != null
                ? qualification.getQualificationScore()
                : qualification.getConfidenceScore();
        boolean shouldQualify = QUALIFY_RECOMMENDATIONS.contains(
                recommendation != null ? recommendation.toUpperCase(Locale.ROOT) : "");

        Map<String, Object> raw = new LinkedHashMap<>();
        if (qualification.getRawStructuredOutput() != null) {
            raw.putAll(qualification.getRawStructuredOutput());
        }
        raw.put("executionId", qualification.getExecutionId() != null
                ? qualification.getExecutionId().toString()
                : null);
        raw.put("promptCode", qualification.getPromptCode());
        raw.put("provider", qualification.getProvider());
        raw.put("confidence", qualification.getConfidence());
        raw.put("reasoning", qualification.getReasoning());
        raw.put("recommendedProduct", qualification.getRecommendedProduct());
        raw.put("recommendedSalesAction", qualification.getRecommendedSalesAction());
        raw.put("riskFactors", qualification.getRiskFactors());
        raw.put("missingInformation", qualification.getMissingInformation());
        raw.put("followUpQuestions", qualification.getFollowUpQuestions());

        int scoreForRecord = suggestedScore != null
                ? suggestedScore
                : confidenceToScore(qualification.getConfidence());
        extensionService.recordQualityScore(leadId, RecordLeadQualityScoreRequest.builder()
                .overallScore(scoreForRecord)
                .nextAction(recommendation)
                .rawLlmResponse(raw)
                .build());

        boolean qualified = false;
        if (shouldQualify) {
            leadService.qualifyLead(leadId, QualifyLeadRequest.builder()
                    .score(scoreForRecord)
                    .notes(StringUtils.hasText(request.getNotes())
                            ? request.getNotes()
                            : "AI qualification: " + recommendation)
                    .build());
            qualified = true;
        } else {
            log.info("AI recommendation {} for lead {} — lead not auto-qualified",
                    recommendation, leadId);
        }

        Lead updated = leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), leadId)
                .orElse(lead);

        return AiLeadQualificationResultDto.builder()
                .lead(leadMapper.toDto(updated))
                .executionId(qualification.getExecutionId())
                .promptCode(qualification.getPromptCode())
                .promptVersion(qualification.getPromptVersion())
                .provider(qualification.getProvider())
                .recommendation(recommendation)
                .confidence(qualification.getConfidence())
                .suggestedScore(scoreForRecord)
                .qualified(qualified)
                .variablesUsed(variables)
                .renderedUserPrompt(null)
                .notes(request.getNotes())
                .qualification(qualification)
                .build();
    }

    private QualificationResultDto executeQualify(
            IndustryQualificationConfigResolver.Resolved config,
            Map<String, String> variables,
            UUID leadId) {
        ApiResponse<QualificationResultDto> response = aiServiceClient.qualify(QualifyLeadAiRequest.builder()
                .promptCode(config.promptCode())
                .capability("LEAD_QUALIFICATION")
                .variables(variables)
                .businessReference(leadId.toString())
                .leadId(leadId.toString())
                .customerId(variables.get("customerId"))
                .build());
        QualificationResultDto data = response != null ? response.getData() : null;
        if (data == null) {
            throw new ValidationException("AI Gateway returned empty qualification response");
        }
        return data;
    }

    private static void putContext(Map<String, String> variables, String key, String value) {
        if (StringUtils.hasText(value) && !variables.containsKey(key)) {
            variables.put(key, value.trim());
        }
    }

    private static int confidenceToScore(Double confidence) {
        if (confidence == null) {
            return 50;
        }
        return Math.max(0, Math.min(100, (int) Math.round(confidence * 100)));
    }

    private UUID requireTenantId() {
        String tenant = com.aisales.common.core.util.TenantContext.getTenantId();
        if (!StringUtils.hasText(tenant)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(tenant);
    }
}
