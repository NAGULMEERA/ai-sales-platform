package com.aisales.lead.application.service;

import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.client.AiServiceClient;
import com.aisales.common.contracts.lead.AiLeadQualificationResultDto;
import com.aisales.common.contracts.lead.QualifyLeadRequest;
import com.aisales.common.contracts.lead.QualifyLeadWithAiRequest;
import com.aisales.common.contracts.lead.RecordLeadQualityScoreRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.exception.exception.NotFoundException;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Coordinates AI Gateway qualification. AI recommends; Lead aggregate decides.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LeadAiQualificationService {

    private static final Set<String> QUALIFY_RECOMMENDATIONS =
            Set.of("QUALIFY", "QUALIFIED", "APPROVE", "APPROVED");

    private final LeadRepository leadRepository;
    private final LeadQualificationVariableMapper variableMapper;
    private final AiServiceClient aiServiceClient;
    private final LeadService leadService;
    private final LeadExtensionService extensionService;
    private final LeadMapper leadMapper;

    @Transactional
    public AiLeadQualificationResultDto qualifyWithAi(UUID leadId, QualifyLeadWithAiRequest request) {
        Lead lead = leadRepository.findByTenantIdAndIdAndDeletedAtIsNull(requireTenantId(), leadId)
                .orElseThrow(() -> new NotFoundException("Lead not found: " + leadId));

        Map<String, String> variables = variableMapper.toVariables(
                lead.getCustomerName(), lead.getAttributes(), request.getVariableKeys());

        AiExecuteResponse ai = executeAi(request.getPromptCode().trim().toUpperCase(Locale.ROOT),
                variables, leadId.toString());

        String recommendation = extractRecommendation(ai);
        Integer suggestedScore = extractSuggestedScore(ai);
        boolean shouldQualify = QUALIFY_RECOMMENDATIONS.contains(
                recommendation != null ? recommendation.toUpperCase(Locale.ROOT) : "");

        Map<String, Object> raw = new LinkedHashMap<>();
        if (ai.getStructuredOutput() != null) {
            raw.putAll(ai.getStructuredOutput());
        }
        raw.put("executionId", ai.getExecutionId() != null ? ai.getExecutionId().toString() : null);
        raw.put("promptCode", ai.getPromptCode());
        raw.put("provider", ai.getProvider());
        raw.put("confidence", ai.getConfidence());

        int scoreForRecord = suggestedScore != null ? suggestedScore : confidenceToScore(ai.getConfidence());
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
                .executionId(ai.getExecutionId())
                .promptCode(ai.getPromptCode())
                .promptVersion(ai.getPromptVersion())
                .provider(ai.getProvider())
                .recommendation(recommendation)
                .confidence(ai.getConfidence())
                .suggestedScore(scoreForRecord)
                .qualified(qualified)
                .variablesUsed(variables)
                .renderedUserPrompt(ai.getRenderedUserPrompt())
                .notes(request.getNotes())
                .build();
    }

    private AiExecuteResponse executeAi(
            String promptCode, Map<String, String> variables, String businessReference) {
        ApiResponse<AiExecuteResponse> response = aiServiceClient.execute(AiExecuteRequest.builder()
                .promptCode(promptCode)
                .variables(variables)
                .businessReference(businessReference)
                .build());
        AiExecuteResponse data = response != null ? response.getData() : null;
        if (data == null) {
            throw new ValidationException("AI Gateway returned empty qualification response");
        }
        return data;
    }

    private static String extractRecommendation(AiExecuteResponse ai) {
        if (ai.getStructuredOutput() == null) {
            return null;
        }
        Object value = ai.getStructuredOutput().get("recommendation");
        return value != null ? String.valueOf(value) : null;
    }

    private static Integer extractSuggestedScore(AiExecuteResponse ai) {
        if (ai.getStructuredOutput() == null) {
            return null;
        }
        Object value = ai.getStructuredOutput().get("score");
        if (value instanceof Number number) {
            return Math.max(0, Math.min(100, number.intValue()));
        }
        if (value != null) {
            try {
                return Math.max(0, Math.min(100, Integer.parseInt(String.valueOf(value))));
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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
