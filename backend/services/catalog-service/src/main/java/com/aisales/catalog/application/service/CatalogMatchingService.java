package com.aisales.catalog.application.service;

import com.aisales.catalog.domain.entity.CatalogOffer;
import com.aisales.catalog.domain.entity.CatalogProduct;
import com.aisales.catalog.infrastructure.persistence.CatalogOfferRepository;
import com.aisales.catalog.infrastructure.persistence.CatalogProductRepository;
import com.aisales.common.contracts.catalog.CatalogItemStatus;
import com.aisales.common.contracts.catalog.CatalogMatchCandidateDto;
import com.aisales.common.contracts.catalog.CatalogMatchRequest;
import com.aisales.common.contracts.catalog.CatalogMatchResultDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.exception.ValidationException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * Deterministic product/offer matching. No industry concepts; filters on catalog metadata only.
 * AI may later enrich ranking — this service never calls AI providers.
 */
@Service
@RequiredArgsConstructor
public class CatalogMatchingService {

    private final CatalogProductRepository productRepository;
    private final CatalogOfferRepository offerRepository;

    @Transactional(readOnly = true)
    public CatalogMatchResultDto match(CatalogMatchRequest request) {
        UUID tenantId = requireTenantId();
        int limit = request.getLimit() == null ? 10 : Math.min(Math.max(request.getLimit(), 1), 50);
        Instant now = Instant.now();

        List<CatalogProduct> products = productRepository.search(
                        tenantId,
                        CatalogItemStatus.ACTIVE,
                        request.getProductType(),
                        trimToNull(request.getCategory()),
                        trimToNull(request.getKeyword()),
                        PageRequest.of(0, Math.max(limit * 3, 30)))
                .getContent();

        if (products.isEmpty()) {
            return CatalogMatchResultDto.builder()
                    .leadId(request.getLeadId())
                    .candidates(List.of())
                    .build();
        }

        List<UUID> productIds = products.stream().map(CatalogProduct::getId).toList();
        Map<UUID, List<CatalogOffer>> offersByProduct = offerRepository
                .findActiveByTenantAndProductIds(tenantId, productIds)
                .stream()
                .filter(offer -> offer.isCurrentlyValid(now))
                .filter(offer -> matchesCurrency(offer, request.getCurrency()))
                .filter(offer -> matchesMaxPrice(offer, request.getMaxPrice()))
                .collect(Collectors.groupingBy(CatalogOffer::getProductId));

        List<CatalogMatchCandidateDto> candidates = new ArrayList<>();
        for (CatalogProduct product : products) {
            List<CatalogOffer> offers = offersByProduct.getOrDefault(product.getId(), List.of());
            if (offers.isEmpty()) {
                if (request.getMaxPrice() != null || StringUtils.hasText(request.getCurrency())) {
                    continue;
                }
                candidates.add(candidateWithoutOffer(product, request));
                continue;
            }
            CatalogOffer best = offers.stream()
                    .min(Comparator.comparing(CatalogOffer::getUnitPrice))
                    .orElseThrow();
            candidates.add(candidateWithOffer(product, best, request));
        }

        candidates.sort(Comparator.comparingInt(CatalogMatchCandidateDto::getMatchScore).reversed());
        if (candidates.size() > limit) {
            candidates = candidates.subList(0, limit);
        }

        return CatalogMatchResultDto.builder()
                .leadId(request.getLeadId())
                .candidates(List.copyOf(candidates))
                .build();
    }

    private CatalogMatchCandidateDto candidateWithOffer(
            CatalogProduct product, CatalogOffer offer, CatalogMatchRequest request) {
        int score = baseScore(product, request);
        score += 20; // priced offer available
        if (request.getMaxPrice() != null
                && offer.getUnitPrice().compareTo(request.getMaxPrice()) <= 0) {
            score += 15;
        }
        return CatalogMatchCandidateDto.builder()
                .productId(product.getId())
                .productCode(product.getCode())
                .productName(product.getName())
                .productType(product.getProductType())
                .category(product.getCategory())
                .offerId(offer.getId())
                .offerCode(offer.getCode())
                .currency(offer.getCurrency())
                .unitPrice(offer.getUnitPrice())
                .matchScore(Math.min(score, 100))
                .reason("Matched active product with priced offer")
                .build();
    }

    private CatalogMatchCandidateDto candidateWithoutOffer(
            CatalogProduct product, CatalogMatchRequest request) {
        return CatalogMatchCandidateDto.builder()
                .productId(product.getId())
                .productCode(product.getCode())
                .productName(product.getName())
                .productType(product.getProductType())
                .category(product.getCategory())
                .matchScore(Math.min(baseScore(product, request), 100))
                .reason("Matched active product (no priced offer)")
                .build();
    }

    private static int baseScore(CatalogProduct product, CatalogMatchRequest request) {
        int score = 40;
        if (StringUtils.hasText(request.getCategory())
                && request.getCategory().equalsIgnoreCase(nullToEmpty(product.getCategory()))) {
            score += 25;
        }
        if (request.getProductType() != null && request.getProductType() == product.getProductType()) {
            score += 15;
        }
        if (StringUtils.hasText(request.getKeyword())) {
            String keyword = request.getKeyword().toLowerCase(Locale.ROOT);
            String haystack = (nullToEmpty(product.getName()) + " " + nullToEmpty(product.getCode())
                    + " " + nullToEmpty(product.getDescription())).toLowerCase(Locale.ROOT);
            if (haystack.contains(keyword)) {
                score += 15;
            }
        }
        return score;
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

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
