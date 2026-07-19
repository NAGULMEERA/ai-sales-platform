package com.aisales.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.catalog.CatalogMatchCandidateDto;
import com.aisales.common.contracts.catalog.CatalogMatchRequest;
import com.aisales.common.contracts.catalog.CatalogMatchResultDto;
import com.aisales.common.contracts.catalog.CatalogRecommendationRequest;
import com.aisales.common.contracts.catalog.CatalogRecommendationResultDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.CatalogRecommendationGeneratedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

@ExtendWith(MockitoExtension.class)
class CatalogRecommendationServiceTest {

    @Mock private CatalogMatchingService matchingService;
    @Mock private EventPublisher eventPublisher;
    @Mock private PlatformTransactionManager transactionManager;
    @Mock private ObjectProvider<?> platformMetrics;

    private CatalogRecommendationService recommendationService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        when(transactionManager.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        when(platformMetrics.getIfAvailable()).thenReturn(null);
        recommendationService = new CatalogRecommendationService(
                matchingService,
                eventPublisher,
                transactionManager,
                (ObjectProvider) platformMetrics);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldSplitTopRecommendationAndAlternatives() {
        UUID top = UUID.randomUUID();
        UUID alt = UUID.randomUUID();
        when(matchingService.match(any(), any(), any(), any(), any()))
                .thenReturn(CatalogMatchResultDto.builder()
                        .leadId(UUID.randomUUID())
                        .candidates(List.of(
                                CatalogMatchCandidateDto.builder()
                                        .productId(top)
                                        .productCode("A")
                                        .matchScore(90)
                                        .confidenceScore(88)
                                        .build(),
                                CatalogMatchCandidateDto.builder()
                                        .productId(alt)
                                        .productCode("B")
                                        .matchScore(70)
                                        .confidenceScore(60)
                                        .build()))
                        .build());

        CatalogRecommendationResultDto result = recommendationService.recommend(
                CatalogRecommendationRequest.builder()
                        .match(CatalogMatchRequest.builder().limit(5).build())
                        .aiSimilarityByProductId(Map.of(top, 0.92))
                        .includeAlternatives(true)
                        .build());

        assertThat(result.getRecommendations()).hasSize(1);
        assertThat(result.getRecommendations().getFirst().getProductId()).isEqualTo(top);
        assertThat(result.getAlternatives()).hasSize(1);
        assertThat(result.getOverallConfidence()).isEqualTo(0.88);

        ArgumentCaptor<CatalogRecommendationGeneratedEvent> captor =
                ArgumentCaptor.forClass(CatalogRecommendationGeneratedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("CatalogRecommendationGenerated");
        assertThat(captor.getValue().getTopProductId()).isEqualTo(top.toString());
    }
}
