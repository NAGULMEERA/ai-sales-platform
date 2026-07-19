package com.aisales.catalog.application.service;

import com.aisales.common.contracts.catalog.CatalogMatchCandidateDto;
import com.aisales.common.contracts.catalog.CatalogMatchRequest;
import com.aisales.common.contracts.catalog.CatalogMatchResultDto;
import com.aisales.common.contracts.catalog.CatalogRecommendationRequest;
import com.aisales.common.contracts.catalog.CatalogRecommendationResultDto;
import com.aisales.common.contracts.catalog.CatalogScoringWeights;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.CatalogRecommendationGeneratedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

/**
 * Hybrid catalog recommendation: rule/metadata matching + optional AI similarity injection.
 * Never calls LLM providers directly — similarity scores come from the caller / AI Gateway.
 */
@Service
@RequiredArgsConstructor
public class CatalogRecommendationService {

    private final CatalogMatchingService matchingService;
    private final EventPublisher eventPublisher;
    private final PlatformTransactionManager transactionManager;
    private final ObjectProvider<PlatformMetrics> platformMetrics;

    @Transactional
    public CatalogRecommendationResultDto recommend(CatalogRecommendationRequest request) {
        UUID tenantId = requireTenantId();
        if (request == null || request.getMatch() == null) {
            throw new ValidationException("Recommendation match criteria are required");
        }

        CatalogMatchRequest matchRequest = request.getMatch();
        CatalogScoringWeights weights = request.getScoringWeights() != null
                ? request.getScoringWeights()
                : (matchRequest.getScoringWeights() != null
                        ? matchRequest.getScoringWeights()
                        : CatalogScoringWeights.defaults());

        Map<UUID, Double> aiScores = request.getAiSimilarityByProductId() != null
                ? request.getAiSimilarityByProductId()
                : Map.of();
        Map<String, Object> conversation = request.getConversationContext() != null
                ? request.getConversationContext()
                : Map.of();
        Map<String, Object> preferences = new HashMap<>();
        if (request.getCustomerPreferences() != null) {
            preferences.putAll(request.getCustomerPreferences());
        }
        if (matchRequest.getPreferences() != null) {
            preferences.putAll(matchRequest.getPreferences());
        }

        CatalogMatchResultDto matched = matchingService.match(
                matchRequest, aiScores, conversation, preferences, weights);

        List<CatalogMatchCandidateDto> ranked = new ArrayList<>(
                matched.getCandidates() != null ? matched.getCandidates() : List.of());

        List<CatalogMatchCandidateDto> recommendations;
        List<CatalogMatchCandidateDto> alternatives;
        if (request.isIncludeAlternatives() && ranked.size() > 1) {
            recommendations = List.of(ranked.getFirst());
            alternatives = List.copyOf(ranked.subList(1, ranked.size()));
        } else {
            recommendations = List.copyOf(ranked);
            alternatives = List.of();
        }

        Double overallConfidence = recommendations.isEmpty()
                ? 0.0
                : recommendations.getFirst().getConfidenceScore() / 100.0;

        String rationale = recommendations.isEmpty()
                ? "No catalog matches for the provided criteria"
                : "Hybrid ranking applied (rules + metadata"
                        + (aiScores.isEmpty() ? "" : " + AI similarity")
                        + ")";

        CatalogRecommendationResultDto result = CatalogRecommendationResultDto.builder()
                .leadId(matchRequest.getLeadId())
                .customerId(request.getCustomerId() != null
                        ? request.getCustomerId()
                        : matchRequest.getCustomerId())
                .recommendations(recommendations)
                .alternatives(alternatives)
                .rationale(rationale)
                .overallConfidence(overallConfidence)
                .build();

        publishRecommendation(tenantId, result);
        incrementMetric(MetricNames.CATALOG_RECOMMENDATION, tenantId);
        return result;
    }

    private void publishRecommendation(UUID tenantId, CatalogRecommendationResultDto result) {
        String correlationId = CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);
        String topProductId = result.getRecommendations().isEmpty()
                ? null
                : result.getRecommendations().getFirst().getProductId().toString();
        String aggregateId = result.getLeadId() != null
                ? result.getLeadId().toString()
                : (topProductId != null ? topProductId : tenantId.toString());
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                eventPublisher.publish(CatalogRecommendationGeneratedEvent.of(
                        tenantId.toString(),
                        aggregateId,
                        result.getLeadId() != null ? result.getLeadId().toString() : null,
                        String.valueOf(result.getRecommendations().size()),
                        topProductId,
                        result.getOverallConfidence() != null
                                ? String.valueOf(result.getOverallConfidence())
                                : null,
                        correlationId)));
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
