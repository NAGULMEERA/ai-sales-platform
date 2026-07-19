package com.aisales.deal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.catalog.CatalogOfferDto;
import com.aisales.common.contracts.deal.CreateOpportunityRequest;
import com.aisales.common.contracts.deal.CreateQuoteRequest;
import com.aisales.common.contracts.deal.OpportunityDto;
import com.aisales.common.contracts.deal.OpportunityStatus;
import com.aisales.common.contracts.deal.QuoteDto;
import com.aisales.common.contracts.deal.QuoteLineItemRequest;
import com.aisales.common.contracts.deal.QuoteStatus;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.deal.application.mapper.DealMapper;
import com.aisales.deal.domain.entity.Opportunity;
import com.aisales.deal.domain.entity.Quote;
import com.aisales.deal.infrastructure.persistence.OpportunityRepository;
import com.aisales.deal.infrastructure.persistence.OpportunityTimelineRepository;
import com.aisales.deal.infrastructure.persistence.QuoteRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Sprint 5: same Opportunity + Quote APIs for property vs vehicle catalog offers.
 * Industry difference lives in Catalog offer snapshots, not deal contracts.
 */
@ExtendWith(MockitoExtension.class)
class IndustryOpportunityQuoteFlowTest {

    @Mock private OpportunityRepository opportunityRepository;
    @Mock private OpportunityTimelineRepository timelineRepository;
    @Mock private QuoteRepository quoteRepository;
    @Mock private CatalogQuoteGateway catalogQuoteGateway;
    @Mock private EventPublisher eventPublisher;
    @Mock private ObjectProvider<?> platformMetrics;

    private OpportunityService opportunityService;
    private QuoteService quoteService;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        DealMapper mapper = new DealMapper();
        org.mockito.Mockito.lenient().when(platformMetrics.getIfAvailable()).thenReturn(null);
        org.mockito.Mockito.lenient()
                .when(timelineRepository.save(any()))
                .thenAnswer(inv -> inv.getArgument(0));
        opportunityService = new OpportunityService(
                opportunityRepository,
                timelineRepository,
                new OpportunityTimelineRecorder(timelineRepository),
                mapper,
                eventPublisher,
                (ObjectProvider) platformMetrics);
        quoteService = new QuoteService(
                quoteRepository, opportunityService, catalogQuoteGateway, mapper, eventPublisher,
                noopTxManager(), org.mockito.Mockito.mock(QuoteIdempotencyService.class));
    }

    private static org.springframework.transaction.PlatformTransactionManager noopTxManager() {
        return new org.springframework.transaction.support.AbstractPlatformTransactionManager() {
            @Override protected Object doGetTransaction() { return new Object(); }
            @Override protected void doBegin(Object transaction,
                    org.springframework.transaction.TransactionDefinition definition) {}
            @Override protected void doCommit(
                    org.springframework.transaction.support.DefaultTransactionStatus status) {}
            @Override protected void doRollback(
                    org.springframework.transaction.support.DefaultTransactionStatus status) {}
        };
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldQuoteRealEstatePropertyOfferOnSameApis() {
        OpportunityDto opportunity = createOpportunity("Whitefield 3BHK opportunity");
        CatalogOfferDto propertyOffer = CatalogOfferDto.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .code("RE-OFFER-3BHK")
                .name("Whitefield 3BHK Base Offer")
                .currency("INR")
                .unitPrice(new BigDecimal("7500000.0000"))
                .build();

        QuoteDto quote = createQuoteForOffer(opportunity.getId(), propertyOffer);

        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(quote.getLineItems()).hasSize(1);
        assertThat(quote.getLineItems().getFirst().getCode()).isEqualTo("RE-OFFER-3BHK");
        assertThat(quote.getLineItems().getFirst().getName()).contains("Whitefield");
        assertThat(quote.getTotalAmount()).isEqualByComparingTo("7500000.0000");
        verify(catalogQuoteGateway).requireOffers(List.of(propertyOffer.getId()));
    }

    @Test
    void shouldQuoteAutomobileVehicleOfferOnSameApis() {
        OpportunityDto opportunity = createOpportunity("Toyota Camry opportunity");
        CatalogOfferDto vehicleOffer = CatalogOfferDto.builder()
                .id(UUID.randomUUID())
                .productId(UUID.randomUUID())
                .code("AUTO-OFFER-CAMRY")
                .name("Toyota Camry Ex-Showroom")
                .currency("INR")
                .unitPrice(new BigDecimal("1800000.0000"))
                .build();

        QuoteDto quote = createQuoteForOffer(opportunity.getId(), vehicleOffer);

        assertThat(quote.getStatus()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(quote.getLineItems()).hasSize(1);
        assertThat(quote.getLineItems().getFirst().getCode()).isEqualTo("AUTO-OFFER-CAMRY");
        assertThat(quote.getLineItems().getFirst().getName()).contains("Camry");
        assertThat(quote.getTotalAmount()).isEqualByComparingTo("1800000.0000");
        verify(catalogQuoteGateway).requireOffers(List.of(vehicleOffer.getId()));
    }

    private OpportunityDto createOpportunity(String name) {
        when(opportunityRepository.saveAndFlush(any(Opportunity.class))).thenAnswer(inv -> {
            Opportunity o = inv.getArgument(0);
            o.setId(UUID.randomUUID());
            return o;
        });
        OpportunityDto dto = opportunityService.create(CreateOpportunityRequest.builder()
                .customerId(UUID.randomUUID())
                .leadId(UUID.randomUUID())
                .name(name)
                .currency("INR")
                .build());
        assertThat(dto.getStatus()).isEqualTo(OpportunityStatus.OPEN);
        return dto;
    }

    private QuoteDto createQuoteForOffer(UUID opportunityId, CatalogOfferDto offer) {
        Opportunity opportunity = Opportunity.builder()
                .id(opportunityId)
                .tenantId(tenantId)
                .customerId(UUID.randomUUID())
                .name("Opp")
                .currency("INR")
                .status(OpportunityStatus.OPEN)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        // QuoteService calls opportunityService.requireOpportunity — use real service via repo mock
        when(opportunityRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, opportunityId))
                .thenReturn(java.util.Optional.of(opportunity));
        when(quoteRepository.findByTenantIdAndOpportunityIdAndDeletedAtIsNullOrderByQuoteVersionDesc(
                        tenantId, opportunityId))
                .thenReturn(Collections.emptyList());
        when(quoteRepository.findMaxVersion(tenantId, opportunityId)).thenReturn(0);
        when(catalogQuoteGateway.requireOffers(List.of(offer.getId()))).thenReturn(Map.of(offer.getId(), offer));
        when(quoteRepository.saveAndFlush(any(Quote.class))).thenAnswer(inv -> {
            Quote q = inv.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
        });

        return quoteService.create(CreateQuoteRequest.builder()
                .opportunityId(opportunityId)
                .notes("Sprint 5 dual-industry quote proof")
                .lineItems(List.of(QuoteLineItemRequest.builder()
                        .offerId(offer.getId())
                        .productId(offer.getProductId())
                        .quantity(BigDecimal.ONE)
                        .build()))
                .build());
    }
}
