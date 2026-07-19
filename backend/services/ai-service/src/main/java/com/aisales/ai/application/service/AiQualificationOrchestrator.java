package com.aisales.ai.application.service;

import com.aisales.common.contracts.ai.AiExecuteRequest;
import com.aisales.common.contracts.ai.AiExecuteResponse;
import com.aisales.common.contracts.ai.QualificationResultDto;
import com.aisales.common.contracts.ai.QualifyLeadAiRequest;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.AiQualificationCompletedEvent;
import com.aisales.common.events.model.AiRecommendationGeneratedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Production qualification orchestration. Assembles prompt variables / RAG via AI Gateway only —
 * never calls LLM providers directly. Business services remain authoritative for lead state.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQualificationOrchestrator {

    private final AiGatewayService aiGatewayService;
    private final QualificationResponseParser responseParser;
    private final EventPublisher eventPublisher;
    private final PlatformTransactionManager transactionManager;
    private final ObjectProvider<PlatformMetrics> platformMetrics;

    public QualificationResultDto qualify(QualifyLeadAiRequest request) {
        UUID tenantId = requireTenantId();
        if (request == null) {
            throw new ValidationException("Qualification request is required");
        }

        Map<String, String> variables = new HashMap<>();
        if (request.getVariables() != null) {
            variables.putAll(request.getVariables());
        }
        putIfAbsent(variables, "leadId", request.getLeadId());
        putIfAbsent(variables, "customerId", request.getCustomerId());

        String businessReference = StringUtils.hasText(request.getBusinessReference())
                ? request.getBusinessReference().trim()
                : request.getLeadId();

        String capability = StringUtils.hasText(request.getCapability())
                ? request.getCapability().trim()
                : "LEAD_QUALIFICATION";

        AiExecuteResponse executed = aiGatewayService.execute(AiExecuteRequest.builder()
                .promptCode(request.getPromptCode())
                .promptId(request.getPromptId())
                .promptVersion(request.getPromptVersion())
                .industryCode(request.getIndustryCode())
                .languageCode(request.getLanguageCode())
                .capability(capability)
                .variables(variables)
                .knowledgeBaseId(request.getKnowledgeBaseId())
                .retrievalQuery(resolveRetrievalQuery(request, variables))
                .retrievalTopK(request.getRetrievalTopK())
                .businessReference(businessReference)
                .includeRenderedPrompts(false)
                .build());

        QualificationResultDto result = responseParser.parse(executed);
        publishQualificationEvents(tenantId, result, request.getLeadId(), businessReference);
        incrementMetric(MetricNames.AI_QUALIFICATION, tenantId);
        return result;
    }

    private void publishQualificationEvents(
            UUID tenantId, QualificationResultDto result, String leadId, String businessReference) {
        String correlationId = CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            eventPublisher.publish(AiQualificationCompletedEvent.of(
                    tenantId.toString(),
                    result.getExecutionId() != null ? result.getExecutionId().toString() : UUID.randomUUID().toString(),
                    result.getPromptCode(),
                    result.getPromptVersion() != null ? String.valueOf(result.getPromptVersion()) : null,
                    result.getProvider(),
                    result.getModel(),
                    result.getRecommendation(),
                    result.getQualificationScore() != null ? String.valueOf(result.getQualificationScore()) : null,
                    result.getConfidenceScore() != null ? String.valueOf(result.getConfidenceScore()) : null,
                    leadId,
                    result.isCacheHit(),
                    correlationId));
            if (StringUtils.hasText(result.getRecommendation())
                    || StringUtils.hasText(result.getRecommendedSalesAction())) {
                eventPublisher.publish(AiRecommendationGeneratedEvent.of(
                        tenantId.toString(),
                        result.getExecutionId() != null
                                ? result.getExecutionId().toString()
                                : UUID.randomUUID().toString(),
                        "LEAD_QUALIFICATION",
                        StringUtils.hasText(result.getRecommendedSalesAction())
                                ? result.getRecommendedSalesAction()
                                : result.getRecommendation(),
                        result.getConfidence() != null
                                ? String.valueOf(result.getConfidence())
                                : (result.getConfidenceScore() != null
                                        ? String.valueOf(result.getConfidenceScore())
                                        : null),
                        businessReference,
                        correlationId));
            }
        });
    }

    private static String resolveRetrievalQuery(QualifyLeadAiRequest request, Map<String, String> variables) {
        if (StringUtils.hasText(request.getRetrievalQuery())) {
            return request.getRetrievalQuery().trim();
        }
        StringBuilder query = new StringBuilder("lead qualification");
        append(query, variables.get("leadName"));
        append(query, variables.get("budget"));
        append(query, variables.get("location"));
        append(query, variables.get("timeline"));
        append(query, variables.get("vehicle"));
        return query.toString().trim();
    }

    private static void append(StringBuilder builder, String value) {
        if (StringUtils.hasText(value)) {
            builder.append(' ').append(value.trim());
        }
    }

    private static void putIfAbsent(Map<String, String> map, String key, String value) {
        if (StringUtils.hasText(value) && !map.containsKey(key)) {
            map.put(key, value.trim());
        }
    }

    private void incrementMetric(String name, UUID tenantId) {
        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        if (metrics != null) {
            metrics.incrementForTenant(name, tenantId.toString());
        }
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }
}
