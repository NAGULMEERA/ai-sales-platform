package com.aisales.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import com.aisales.catalog.domain.entity.CatalogOffer;
import com.aisales.catalog.domain.entity.CatalogProduct;
import com.aisales.catalog.infrastructure.persistence.CatalogOfferRepository;
import com.aisales.catalog.infrastructure.persistence.CatalogProductRepository;
import com.aisales.common.contracts.catalog.CatalogItemStatus;
import com.aisales.common.contracts.catalog.CatalogMatchRequest;
import com.aisales.common.contracts.catalog.CatalogMatchResultDto;
import com.aisales.common.contracts.catalog.CatalogProductType;
import com.aisales.common.core.util.TenantContext;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.aisales.common.events.publisher.EventPublisher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class CatalogMatchingServiceTest {

    @Mock private CatalogProductRepository productRepository;
    @Mock private CatalogOfferRepository offerRepository;
    @Mock private EventPublisher eventPublisher;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ObjectProvider<?> platformMetrics;

    private CatalogMatchingService matchingService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        org.mockito.Mockito.lenient()
                .when(transactionManager.getTransaction(org.mockito.ArgumentMatchers.any()))
                .thenReturn(new SimpleTransactionStatus());
        org.mockito.Mockito.lenient().when(platformMetrics.getIfAvailable()).thenReturn(null);
        matchingService = new CatalogMatchingService(
                productRepository,
                offerRepository,
                eventPublisher,
                transactionManager,
                (ObjectProvider) platformMetrics);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldRankProductWithOfferWithinBudget() {
        UUID productId = UUID.randomUUID();
        CatalogProduct product = CatalogProduct.builder()
                .id(productId)
                .tenantId(tenantId)
                .code("SKU-A")
                .name("Lake View")
                .category("residential")
                .productType(CatalogProductType.PRODUCT)
                .status(CatalogItemStatus.ACTIVE)
                .build();
        CatalogOffer offer = CatalogOffer.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .productId(productId)
                .code("OFF-A")
                .name("Base")
                .currency("INR")
                .unitPrice(new BigDecimal("2000000"))
                .status(CatalogItemStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(productRepository.search(
                eq(tenantId),
                eq(CatalogItemStatus.ACTIVE),
                eq(CatalogProductType.PRODUCT),
                eq("residential"),
                any(),
                any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(offerRepository.findActiveByTenantAndProductIds(eq(tenantId), any()))
                .thenReturn(List.of(offer));

        CatalogMatchResultDto result = matchingService.match(CatalogMatchRequest.builder()
                .productType(CatalogProductType.PRODUCT)
                .category("residential")
                .maxPrice(new BigDecimal("2500000"))
                .currency("INR")
                .limit(5)
                .build());

        assertThat(result.getCandidates()).hasSize(1);
        assertThat(result.getCandidates().getFirst().getProductId()).isEqualTo(productId);
        assertThat(result.getCandidates().getFirst().getOfferId()).isEqualTo(offer.getId());
        assertThat(result.getCandidates().getFirst().getMatchScore()).isGreaterThan(50);
    }

    @Test
    void shouldExcludeOffersAboveMaxPrice() {
        UUID productId = UUID.randomUUID();
        CatalogProduct product = CatalogProduct.builder()
                .id(productId)
                .tenantId(tenantId)
                .code("SKU-B")
                .name("Penthouse")
                .productType(CatalogProductType.PRODUCT)
                .status(CatalogItemStatus.ACTIVE)
                .build();
        CatalogOffer expensive = CatalogOffer.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .productId(productId)
                .code("OFF-B")
                .name("Premium")
                .currency("INR")
                .unitPrice(new BigDecimal("9000000"))
                .status(CatalogItemStatus.ACTIVE)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(productRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(product)));
        when(offerRepository.findActiveByTenantAndProductIds(eq(tenantId), any()))
                .thenReturn(List.of(expensive));

        CatalogMatchResultDto result = matchingService.match(CatalogMatchRequest.builder()
                .maxPrice(new BigDecimal("1000000"))
                .currency("INR")
                .build());

        assertThat(result.getCandidates()).isEmpty();
    }

    @Test
    void shouldMatchRealEstateProductsByAttributeFiltersOnSameApi() {
        CatalogProduct matching = product("RE-APT-3BHK", "Whitefield Apartment", "residential", Map.of(
                "bedrooms", 3,
                "bathrooms", 2,
                "location", "Whitefield",
                "price", 7500000));
        CatalogProduct other = product("RE-APT-2BHK", "Koramangala Flat", "residential", Map.of(
                "bedrooms", 2,
                "bathrooms", 2,
                "location", "Koramangala",
                "price", 6500000));

        when(productRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(matching, other)));
        when(offerRepository.findActiveByTenantAndProductIds(eq(tenantId), any()))
                .thenReturn(List.of());

        CatalogMatchResultDto result = matchingService.match(CatalogMatchRequest.builder()
                .category("residential")
                .attributeFilters(Map.of(
                        "bedrooms", 3,
                        "location", "Whitefield"))
                .limit(10)
                .build());

        assertThat(result.getCandidates()).hasSize(1);
        assertThat(result.getCandidates().getFirst().getProductCode()).isEqualTo("RE-APT-3BHK");
        assertThat(result.getCandidates().getFirst().getReason()).contains("attribute filters");
        assertThat(result.getCandidates().getFirst().getMatchScore()).isGreaterThan(40);
    }

    @Test
    void shouldMatchAutomobileProductsByAttributeFiltersOnSameApi() {
        CatalogProduct matching = product("AUTO-CAMRY", "Toyota Camry", "vehicle", Map.of(
                "make", "Toyota",
                "model", "Camry",
                "year", 2024,
                "price", 1800000));
        CatalogProduct other = product("AUTO-CITY", "Honda City", "vehicle", Map.of(
                "make", "Honda",
                "model", "City",
                "year", 2023,
                "price", 1200000));

        when(productRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(matching, other)));
        when(offerRepository.findActiveByTenantAndProductIds(eq(tenantId), any()))
                .thenReturn(List.of());

        CatalogMatchResultDto result = matchingService.match(CatalogMatchRequest.builder()
                .category("vehicle")
                .attributeFilters(Map.of(
                        "make", "Toyota",
                        "model", "Camry",
                        "year", 2024))
                .limit(10)
                .build());

        assertThat(result.getCandidates()).hasSize(1);
        assertThat(result.getCandidates().getFirst().getProductCode()).isEqualTo("AUTO-CAMRY");
        assertThat(result.getCandidates().getFirst().getReason())
                .contains("make")
                .contains("model")
                .contains("year");
    }

    @Test
    void shouldExcludeProductsMissingRequiredAttributeFilters() {
        CatalogProduct withoutLocation = product("RE-NO-LOC", "Mystery Unit", "residential", Map.of(
                "bedrooms", 3,
                "bathrooms", 2));

        when(productRepository.search(any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(withoutLocation)));

        CatalogMatchResultDto result = matchingService.match(CatalogMatchRequest.builder()
                .attributeFilters(Map.of("bedrooms", 3, "location", "Whitefield"))
                .build());

        assertThat(result.getCandidates()).isEmpty();
    }

    private CatalogProduct product(String code, String name, String category, Map<String, Object> attributes) {
        return CatalogProduct.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .code(code)
                .name(name)
                .category(category)
                .productType(CatalogProductType.PRODUCT)
                .status(CatalogItemStatus.ACTIVE)
                .attributes(new HashMap<>(attributes))
                .build();
    }
}
