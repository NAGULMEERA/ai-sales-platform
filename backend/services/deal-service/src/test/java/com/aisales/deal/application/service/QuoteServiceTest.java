package com.aisales.deal.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.catalog.CatalogOfferDto;
import com.aisales.common.contracts.deal.CreateQuoteRequest;
import com.aisales.common.contracts.deal.OpportunityStatus;
import com.aisales.common.contracts.deal.QuoteDto;
import com.aisales.common.contracts.deal.QuoteLineItemRequest;
import com.aisales.common.contracts.deal.QuoteStatus;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.QuoteAcceptedEvent;
import com.aisales.common.events.model.QuoteCreatedEvent;
import com.aisales.common.events.model.QuoteSentEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.deal.application.mapper.DealMapper;
import com.aisales.deal.domain.entity.Opportunity;
import com.aisales.deal.domain.entity.Quote;
import com.aisales.deal.infrastructure.persistence.QuoteRepository;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
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
class QuoteServiceTest {

    @Mock private QuoteRepository quoteRepository;
    @Mock private OpportunityService opportunityService;
    @Mock private CatalogQuoteGateway catalogQuoteGateway;
    @Mock private EventPublisher eventPublisher;

    private QuoteService quoteService;
    private UUID tenantId;
    private UUID opportunityId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        opportunityId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        quoteService = new QuoteService(
                quoteRepository,
                opportunityService,
                catalogQuoteGateway,
                new DealMapper(),
                eventPublisher);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldCreateQuoteWithCatalogSnapshot() {
        Opportunity opportunity = openOpportunity();
        when(opportunityService.requireOpportunity(opportunityId)).thenReturn(opportunity);
        when(quoteRepository.findByTenantIdAndOpportunityIdAndDeletedAtIsNullOrderByQuoteVersionDesc(
                        tenantId, opportunityId))
                .thenReturn(Collections.emptyList());
        when(quoteRepository.findMaxVersion(tenantId, opportunityId)).thenReturn(0);

        UUID offerId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(catalogQuoteGateway.requireOffer(offerId)).thenReturn(CatalogOfferDto.builder()
                .id(offerId)
                .productId(productId)
                .code("OFFER-1")
                .name("Studio Plan")
                .currency("INR")
                .unitPrice(new BigDecimal("2500000.0000"))
                .build());
        when(quoteRepository.saveAndFlush(any(Quote.class))).thenAnswer(inv -> {
            Quote q = inv.getArgument(0);
            q.setId(UUID.randomUUID());
            return q;
        });

        QuoteDto dto = quoteService.create(CreateQuoteRequest.builder()
                .opportunityId(opportunityId)
                .lineItems(List.of(QuoteLineItemRequest.builder()
                        .offerId(offerId)
                        .quantity(BigDecimal.ONE)
                        .build()))
                .build());

        assertThat(dto.getStatus()).isEqualTo(QuoteStatus.DRAFT);
        assertThat(dto.getTotalAmount()).isEqualByComparingTo("2500000.0000");
        assertThat(dto.getLineItems()).hasSize(1);
        assertThat(dto.getLineItems().get(0).getCode()).isEqualTo("OFFER-1");

        ArgumentCaptor<QuoteCreatedEvent> captor = ArgumentCaptor.forClass(QuoteCreatedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("QuoteCreated");
    }

    @Test
    void shouldRequireOfferIdForPricing() {
        when(opportunityService.requireOpportunity(opportunityId)).thenReturn(openOpportunity());
        when(quoteRepository.findByTenantIdAndOpportunityIdAndDeletedAtIsNullOrderByQuoteVersionDesc(
                        tenantId, opportunityId))
                .thenReturn(Collections.emptyList());
        when(quoteRepository.findMaxVersion(tenantId, opportunityId)).thenReturn(0);
        when(catalogQuoteGateway.requireProduct(any())).thenReturn(
                com.aisales.common.contracts.catalog.CatalogProductDto.builder()
                        .id(UUID.randomUUID())
                        .code("SKU-1")
                        .name("Product")
                        .build());

        assertThatThrownBy(() -> quoteService.create(CreateQuoteRequest.builder()
                        .opportunityId(opportunityId)
                        .lineItems(List.of(QuoteLineItemRequest.builder()
                                .productId(UUID.randomUUID())
                                .quantity(BigDecimal.ONE)
                                .build()))
                        .build()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("offerId is required");
    }

    @Test
    void shouldSendQuoteAndMarkOpportunityQuoted() {
        UUID quoteId = UUID.randomUUID();
        Quote quote = draftQuote(quoteId);
        when(quoteRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, quoteId))
                .thenReturn(Optional.of(quote));
        when(quoteRepository.save(any(Quote.class))).thenAnswer(inv -> inv.getArgument(0));

        QuoteDto dto = quoteService.send(quoteId);

        assertThat(dto.getStatus()).isEqualTo(QuoteStatus.SENT);
        verify(opportunityService).markQuoted(opportunityId);
        ArgumentCaptor<QuoteSentEvent> captor = ArgumentCaptor.forClass(QuoteSentEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("QuoteSent");
    }

    @Test
    void shouldAcceptQuoteAndMarkOpportunityWon() {
        UUID quoteId = UUID.randomUUID();
        Quote quote = draftQuote(quoteId);
        quote.setStatus(QuoteStatus.SENT);
        when(quoteRepository.findByTenantIdAndIdAndDeletedAtIsNull(tenantId, quoteId))
                .thenReturn(Optional.of(quote));
        when(quoteRepository.save(any(Quote.class))).thenAnswer(inv -> inv.getArgument(0));

        QuoteDto dto = quoteService.accept(quoteId);

        assertThat(dto.getStatus()).isEqualTo(QuoteStatus.ACCEPTED);
        verify(opportunityService).markWon(opportunityId, "quote accepted");
        ArgumentCaptor<QuoteAcceptedEvent> captor = ArgumentCaptor.forClass(QuoteAcceptedEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("QuoteAccepted");
    }

    private Opportunity openOpportunity() {
        return Opportunity.builder()
                .id(opportunityId)
                .tenantId(tenantId)
                .customerId(UUID.randomUUID())
                .name("Opp")
                .currency("INR")
                .status(OpportunityStatus.OPEN)
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
    }

    private Quote draftQuote(UUID quoteId) {
        Quote quote = Quote.builder()
                .id(quoteId)
                .tenantId(tenantId)
                .opportunityId(opportunityId)
                .quoteVersion(1)
                .status(QuoteStatus.DRAFT)
                .currency("INR")
                .totalAmount(new BigDecimal("100.0000"))
                .createdAt(java.time.Instant.now())
                .updatedAt(java.time.Instant.now())
                .build();
        quote.addLineItem(com.aisales.deal.domain.entity.QuoteLineItem.of(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "OFFER-1",
                "Item",
                BigDecimal.ONE,
                new BigDecimal("100.0000"),
                "INR"));
        return quote;
    }
}
