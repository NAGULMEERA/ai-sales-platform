package com.aisales.catalog.application.service;

import com.aisales.catalog.domain.entity.CatalogOffer;
import com.aisales.catalog.domain.entity.CatalogProduct;
import com.aisales.catalog.infrastructure.persistence.CatalogOfferRepository;
import com.aisales.catalog.infrastructure.persistence.CatalogProductRepository;
import com.aisales.common.contracts.catalog.CatalogItemStatus;
import com.aisales.common.contracts.catalog.CatalogMatchCandidateDto;
import com.aisales.common.contracts.catalog.CatalogMatchRequest;
import com.aisales.common.contracts.catalog.CatalogMatchResultDto;
import com.aisales.common.contracts.catalog.CatalogMatchScoreFactorsDto;
import com.aisales.common.contracts.catalog.CatalogScoringWeights;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.CatalogMatchedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.util.StringUtils;

/**
 * Deterministic + hybrid factor catalog matching. Industry specifics stay in attribute metadata.
 * Does not call LLM providers — AI similarity may be injected by {@link CatalogRecommendationService}.
 */
@Service
@RequiredArgsConstructor
public class CatalogMatchingService {

    private final CatalogProductRepository productRepository;
    private final CatalogOfferRepository offerRepository;
    private final EventPublisher eventPublisher;
    private final PlatformTransactionManager transactionManager;
    private final ObjectProvider<PlatformMetrics> platformMetrics;

    @Transactional
    public CatalogMatchResultDto match(CatalogMatchRequest request) {
        return match(request, Map.of(), Map.of(), Map.of(), null);
    }

    CatalogMatchResultDto match(
            CatalogMatchRequest request,
            Map<UUID, Double> aiSimilarityByProductId,
            Map<String, Object> conversationContext,
            Map<String, Object> customerPreferences,
            CatalogScoringWeights overrideWeights) {
        UUID tenantId = requireTenantId();
        if (request == null) {
            throw new ValidationException("Match request is required");
        }
        int limit = request.getLimit() == null ? 10 : Math.min(Math.max(request.getLimit(), 1), 50);
        Instant now = Instant.now();
        Map<String, Object> attributeFilters = normalizeFilters(request.getAttributeFilters());
        Map<String, Object> preferences = normalizeFilters(request.getPreferences());
        if (StringUtils.hasText(request.getLocation()) && !attributeFilters.containsKey("location")) {
            // location soft-preference when not already a hard filter
            preferences = new LinkedHashMap<>(preferences);
            preferences.putIfAbsent("location", request.getLocation().trim());
        }
        BigDecimal maxPrice = resolveMaxPrice(request);
        CatalogScoringWeights weights = overrideWeights != null
                ? overrideWeights
                : (request.getScoringWeights() != null
                        ? request.getScoringWeights()
                        : CatalogScoringWeights.defaults());

        List<CatalogProduct> products = productRepository.search(
                        tenantId,
                        CatalogItemStatus.ACTIVE,
                        request.getProductType(),
                        trimToNull(request.getCategory()),
                        trimToNull(request.getKeyword()),
                        PageRequest.of(0, Math.max(limit * 3, 30)))
                .getContent()
                .stream()
                .filter(product -> matchesAttributeFilters(product, attributeFilters))
                .toList();

        if (products.isEmpty()) {
            CatalogMatchResultDto empty = CatalogMatchResultDto.builder()
                    .leadId(request.getLeadId())
                    .candidates(List.of())
                    .build();
            publishMatched(tenantId, request.getLeadId(), 0);
            return empty;
        }

        List<UUID> productIds = products.stream().map(CatalogProduct::getId).toList();
        Map<UUID, List<CatalogOffer>> offersByProduct = offerRepository
                .findActiveByTenantAndProductIds(tenantId, productIds)
                .stream()
                .filter(offer -> offer.isCurrentlyValid(now))
                .filter(offer -> matchesCurrency(offer, request.getCurrency()))
                .filter(offer -> matchesMaxPrice(offer, maxPrice))
                .collect(Collectors.groupingBy(CatalogOffer::getProductId));

        List<CatalogMatchCandidateDto> candidates = new ArrayList<>();
        for (CatalogProduct product : products) {
            List<CatalogOffer> offers = offersByProduct.getOrDefault(product.getId(), List.of());
            if (offers.isEmpty()) {
                if (maxPrice != null || StringUtils.hasText(request.getCurrency())) {
                    continue;
                }
                candidates.add(scoreCandidate(
                        product,
                        null,
                        request,
                        attributeFilters,
                        preferences,
                        conversationContext,
                        customerPreferences,
                        aiSimilarityByProductId,
                        weights));
                continue;
            }
            CatalogOffer best = offers.stream()
                    .min(Comparator.comparing(CatalogOffer::getUnitPrice))
                    .orElseThrow();
            candidates.add(scoreCandidate(
                    product,
                    best,
                    request,
                    attributeFilters,
                    preferences,
                    conversationContext,
                    customerPreferences,
                    aiSimilarityByProductId,
                    weights));
        }

        candidates.sort(Comparator
                .comparingInt(CatalogMatchCandidateDto::getMatchScore)
                .reversed()
                .thenComparingInt(CatalogMatchCandidateDto::getConfidenceScore)
                .reversed());
        if (candidates.size() > limit) {
            candidates = candidates.subList(0, limit);
        }

        CatalogMatchResultDto result = CatalogMatchResultDto.builder()
                .leadId(request.getLeadId())
                .candidates(List.copyOf(candidates))
                .build();
        publishMatched(tenantId, request.getLeadId(), candidates.size());
        incrementMetric(MetricNames.CATALOG_MATCH, tenantId);
        return result;
    }

    private CatalogMatchCandidateDto scoreCandidate(
            CatalogProduct product,
            CatalogOffer offer,
            CatalogMatchRequest request,
            Map<String, Object> attributeFilters,
            Map<String, Object> preferences,
            Map<String, Object> conversationContext,
            Map<String, Object> customerPreferences,
            Map<UUID, Double> aiSimilarityByProductId,
            CatalogScoringWeights weights) {
        CatalogMatchScoreFactorsDto factors = CatalogMatchScoreFactorsDto.builder()
                .budgetMatch(scoreBudget(product, offer, resolveMaxPrice(request)))
                .locationMatch(scoreLocation(product, request.getLocation(), preferences, attributeFilters))
                .featureMatch(scoreFeatures(product, attributeFilters, preferences))
                .availability(scoreAvailability(product, offer))
                .leadIntent(scoreLeadIntent(request, conversationContext))
                .conversationContext(scoreMapOverlap(product, conversationContext))
                .customerPreferences(scoreMapOverlap(product, customerPreferences))
                .aiSimilarity(scoreAiSimilarity(product.getId(), aiSimilarityByProductId))
                .build();

        int matchScore = weightedScore(factors, weights);
        int confidence = confidenceFromFactors(factors, offer != null);
        List<String> reasons = buildReasons(factors, offer != null, attributeFilters);

        return CatalogMatchCandidateDto.builder()
                .productId(product.getId())
                .productCode(product.getCode())
                .productName(product.getName())
                .productType(product.getProductType())
                .category(product.getCategory())
                .offerId(offer != null ? offer.getId() : null)
                .offerCode(offer != null ? offer.getCode() : null)
                .currency(offer != null ? offer.getCurrency() : null)
                .unitPrice(offer != null ? offer.getUnitPrice() : null)
                .matchScore(matchScore)
                .confidenceScore(confidence)
                .scoreFactors(factors)
                .reason(String.join("; ", reasons))
                .reasons(reasons)
                .build();
    }

    static int weightedScore(CatalogMatchScoreFactorsDto f, CatalogScoringWeights w) {
        int totalWeight = w.getBudget() + w.getLocation() + w.getFeature() + w.getAvailability()
                + w.getLeadIntent() + w.getConversation() + w.getPreferences() + w.getAiSimilarity();
        if (totalWeight <= 0) {
            totalWeight = 100;
        }
        double raw = (f.getBudgetMatch() * w.getBudget()
                + f.getLocationMatch() * w.getLocation()
                + f.getFeatureMatch() * w.getFeature()
                + f.getAvailability() * w.getAvailability()
                + f.getLeadIntent() * w.getLeadIntent()
                + f.getConversationContext() * w.getConversation()
                + f.getCustomerPreferences() * w.getPreferences()
                + f.getAiSimilarity() * w.getAiSimilarity()) / (double) totalWeight;
        return clamp((int) Math.round(raw));
    }

    private static int confidenceFromFactors(CatalogMatchScoreFactorsDto f, boolean withOffer) {
        int base = (f.getFeatureMatch() + f.getBudgetMatch() + f.getAvailability()) / 3;
        if (withOffer) {
            base += 5;
        }
        if (f.getAiSimilarity() >= 70) {
            base += 10;
        }
        return clamp(base);
    }

    private static int scoreBudget(CatalogProduct product, CatalogOffer offer, BigDecimal maxPrice) {
        if (maxPrice == null) {
            return 60;
        }
        BigDecimal price = offer != null ? offer.getUnitPrice() : attributeDecimal(product, "price");
        if (price == null) {
            return 40;
        }
        if (price.compareTo(maxPrice) > 0) {
            return 0;
        }
        // closer to budget (without exceeding) scores higher
        BigDecimal ratio = price.divide(maxPrice, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.5")) < 0) {
            return 70;
        }
        if (ratio.compareTo(new BigDecimal("0.9")) <= 0) {
            return 100;
        }
        return 85;
    }

    private static int scoreLocation(
            CatalogProduct product,
            String location,
            Map<String, Object> preferences,
            Map<String, Object> filters) {
        String wanted = firstText(location, preferences.get("location"), filters.get("location"));
        if (!StringUtils.hasText(wanted)) {
            return 50;
        }
        Object productLocation = product.getAttributes() != null
                ? product.getAttributes().get("location")
                : null;
        if (productLocation == null) {
            return 20;
        }
        return String.valueOf(productLocation).trim().equalsIgnoreCase(wanted.trim()) ? 100 : 25;
    }

    private static int scoreFeatures(
            CatalogProduct product, Map<String, Object> filters, Map<String, Object> preferences) {
        Map<String, Object> attrs = product.getAttributes() != null ? product.getAttributes() : Map.of();
        int checks = 0;
        int hits = 0;
        for (Map.Entry<String, Object> e : filters.entrySet()) {
            if ("location".equalsIgnoreCase(e.getKey()) || "price".equalsIgnoreCase(e.getKey())) {
                continue;
            }
            checks++;
            if (attributeValueMatches(attrs.get(e.getKey()), e.getValue())) {
                hits++;
            }
        }
        for (Map.Entry<String, Object> e : preferences.entrySet()) {
            if ("location".equalsIgnoreCase(e.getKey()) || filters.containsKey(e.getKey())) {
                continue;
            }
            checks++;
            if (attributeValueMatches(attrs.get(e.getKey()), e.getValue())) {
                hits++;
            }
        }
        if (checks == 0) {
            return 55;
        }
        return clamp((int) Math.round((hits * 100.0) / checks));
    }

    private static int scoreAvailability(CatalogProduct product, CatalogOffer offer) {
        if (product.getStatus() != CatalogItemStatus.ACTIVE) {
            return 0;
        }
        Object available = product.getAttributes() != null
                ? product.getAttributes().get("availability")
                : null;
        if (available != null) {
            String text = String.valueOf(available).trim().toUpperCase(Locale.ROOT);
            if (text.equals("UNAVAILABLE") || text.equals("SOLD") || text.equals("FALSE") || text.equals("0")) {
                return 0;
            }
            if (text.equals("AVAILABLE") || text.equals("TRUE") || text.equals("1") || text.equals("IN_STOCK")) {
                return offer != null ? 100 : 80;
            }
        }
        return offer != null ? 90 : 55;
    }

    private static int scoreLeadIntent(CatalogMatchRequest request, Map<String, Object> conversationContext) {
        int score = 40;
        if (request.getLeadId() != null) {
            score += 20;
        }
        if (StringUtils.hasText(request.getKeyword())) {
            score += 15;
        }
        if (conversationContext != null && !conversationContext.isEmpty()) {
            score += 15;
        }
        Object intent = conversationContext != null ? conversationContext.get("intent") : null;
        if (intent != null && String.valueOf(intent).toUpperCase(Locale.ROOT).contains("BUY")) {
            score += 10;
        }
        return clamp(score);
    }

    private static int scoreMapOverlap(CatalogProduct product, Map<String, Object> signals) {
        if (signals == null || signals.isEmpty()) {
            return 40;
        }
        Map<String, Object> attrs = product.getAttributes() != null ? product.getAttributes() : Map.of();
        int hits = 0;
        int checks = 0;
        for (Map.Entry<String, Object> e : signals.entrySet()) {
            if (e.getValue() == null || "intent".equalsIgnoreCase(e.getKey())) {
                continue;
            }
            checks++;
            if (attributeValueMatches(attrs.get(e.getKey()), e.getValue())
                    || (product.getCategory() != null
                            && product.getCategory().equalsIgnoreCase(String.valueOf(e.getValue())))) {
                hits++;
            }
        }
        if (checks == 0) {
            return 45;
        }
        return clamp((int) Math.round((hits * 100.0) / checks));
    }

    private static int scoreAiSimilarity(UUID productId, Map<UUID, Double> aiSimilarityByProductId) {
        if (aiSimilarityByProductId == null || productId == null) {
            return 0;
        }
        Double similarity = aiSimilarityByProductId.get(productId);
        if (similarity == null) {
            return 0;
        }
        double clamped = Math.max(0.0, Math.min(1.0, similarity));
        return clamp((int) Math.round(clamped * 100));
    }

    private static List<String> buildReasons(
            CatalogMatchScoreFactorsDto factors, boolean withOffer, Map<String, Object> attributeFilters) {
        List<String> reasons = new ArrayList<>();
        reasons.add(withOffer
                ? "Matched active product with priced offer"
                : "Matched active product (no priced offer)");
        if (factors.getBudgetMatch() >= 80) {
            reasons.add("Strong budget fit");
        }
        if (factors.getLocationMatch() >= 80) {
            reasons.add("Location match");
        }
        if (factors.getFeatureMatch() >= 80) {
            reasons.add("Feature match");
        }
        if (factors.getAvailability() >= 80) {
            reasons.add("Available");
        }
        if (factors.getAiSimilarity() >= 70) {
            reasons.add("AI similarity");
        }
        if (!attributeFilters.isEmpty()) {
            reasons.add("attribute filters: " + String.join(", ", attributeFilters.keySet()));
        }
        return reasons;
    }

    static boolean matchesAttributeFilters(CatalogProduct product, Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return true;
        }
        Map<String, Object> attributes = product.getAttributes() != null
                ? product.getAttributes()
                : Map.of();
        for (Map.Entry<String, Object> filter : filters.entrySet()) {
            if (!attributeValueMatches(attributes.get(filter.getKey()), filter.getValue())) {
                return false;
            }
        }
        return true;
    }

    static boolean attributeValueMatches(Object productValue, Object filterValue) {
        if (filterValue == null) {
            return true;
        }
        if (productValue == null) {
            return false;
        }
        if (productValue instanceof Number || filterValue instanceof Number) {
            BigDecimal productNumber = toBigDecimal(productValue);
            BigDecimal filterNumber = toBigDecimal(filterValue);
            if (productNumber != null && filterNumber != null) {
                return productNumber.compareTo(filterNumber) == 0;
            }
        }
        if (productValue instanceof Boolean || filterValue instanceof Boolean) {
            return Boolean.valueOf(String.valueOf(productValue))
                    .equals(Boolean.valueOf(String.valueOf(filterValue)));
        }
        return String.valueOf(productValue).trim().equalsIgnoreCase(String.valueOf(filterValue).trim());
    }

    private static BigDecimal attributeDecimal(CatalogProduct product, String key) {
        if (product.getAttributes() == null) {
            return null;
        }
        return toBigDecimal(product.getAttributes().get(key));
    }

    private static BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(String.valueOf(value).trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static BigDecimal resolveMaxPrice(CatalogMatchRequest request) {
        if (request.getMaxPrice() != null) {
            return request.getMaxPrice();
        }
        return request.getBudget();
    }

    private static Map<String, Object> normalizeFilters(Map<String, Object> filters) {
        if (filters == null || filters.isEmpty()) {
            return Map.of();
        }
        Map<String, Object> normalized = new LinkedHashMap<>();
        filters.forEach((key, value) -> {
            if (StringUtils.hasText(key) && value != null) {
                normalized.put(key.trim(), value);
            }
        });
        return normalized;
    }

    private static boolean matchesCurrency(CatalogOffer offer, String currency) {
        if (!StringUtils.hasText(currency)) {
            return true;
        }
        return offer.getCurrency().equalsIgnoreCase(currency.trim());
    }

    private static boolean matchesMaxPrice(CatalogOffer offer, BigDecimal maxPrice) {
        if (maxPrice == null) {
            return true;
        }
        return offer.getUnitPrice().compareTo(maxPrice) <= 0;
    }

    private void publishMatched(UUID tenantId, UUID leadId, int candidateCount) {
        String correlationId = CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);
        new TransactionTemplate(transactionManager).executeWithoutResult(status ->
                eventPublisher.publish(CatalogMatchedEvent.of(
                        tenantId.toString(),
                        leadId != null ? leadId.toString() : tenantId.toString(),
                        String.valueOf(candidateCount),
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

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private static String firstText(Object... values) {
        for (Object value : values) {
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    private static int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }
}
