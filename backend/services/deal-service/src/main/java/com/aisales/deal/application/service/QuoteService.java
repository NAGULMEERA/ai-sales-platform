package com.aisales.deal.application.service;

import com.aisales.common.contracts.catalog.CatalogOfferDto;
import com.aisales.common.contracts.catalog.CatalogProductDto;
import com.aisales.common.contracts.deal.CreateQuoteRequest;
import com.aisales.common.contracts.deal.QuoteDto;
import com.aisales.common.contracts.deal.QuoteLineItemRequest;
import com.aisales.common.contracts.deal.QuoteStatus;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.QuoteAcceptedEvent;
import com.aisales.common.events.model.QuoteCreatedEvent;
import com.aisales.common.events.model.QuoteSentEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.deal.application.mapper.DealMapper;
import com.aisales.deal.domain.entity.Opportunity;
import com.aisales.deal.domain.entity.Quote;
import com.aisales.deal.domain.entity.QuoteLineItem;
import com.aisales.deal.infrastructure.persistence.QuoteRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class QuoteService {

    private final QuoteRepository quoteRepository;
    private final OpportunityService opportunityService;
    private final CatalogQuoteGateway catalogQuoteGateway;
    private final DealMapper mapper;
    private final EventPublisher eventPublisher;
    private final PlatformTransactionManager transactionManager;
    private final QuoteIdempotencyService quoteIdempotencyService;

    /**
     * Catalog Feign lookups run outside the DB transaction; quote persist is a short local TX.
     */
    public QuoteDto create(CreateQuoteRequest request) {
        return create(request, null);
    }

    public QuoteDto create(CreateQuoteRequest request, String idempotencyKey) {
        if (StringUtils.hasText(idempotencyKey)) {
            var cached = quoteIdempotencyService.findCachedCreateResponse(idempotencyKey);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        UUID tenantId = requireTenantId();
        Opportunity opportunity = opportunityService.requireOpportunity(request.getOpportunityId());
        if (opportunity.isTerminal()) {
            throw new ValidationException("Cannot quote a terminal opportunity");
        }

        String currency = StringUtils.hasText(request.getCurrency())
                ? request.getCurrency().trim().toUpperCase()
                : opportunity.getCurrency();

        List<QuoteLineItem> lineItems = new ArrayList<>();
        for (QuoteLineItemRequest lineRequest : request.getLineItems()) {
            lineItems.add(resolveLineItem(lineRequest, currency));
        }

        return new TransactionTemplate(transactionManager).execute(status -> {
            if (StringUtils.hasText(idempotencyKey)) {
                var cached = quoteIdempotencyService.beginCreate(idempotencyKey);
                if (cached.isPresent()) {
                    return cached.get();
                }
            }
            QuoteDto created = persistNewQuote(tenantId, opportunity.getId(), currency, request, lineItems);
            if (StringUtils.hasText(idempotencyKey)) {
                quoteIdempotencyService.storeCreateResponse(idempotencyKey, created.getId(), created);
            }
            return created;
        });
    }

    private QuoteDto persistNewQuote(
            UUID tenantId,
            UUID opportunityId,
            String currency,
            CreateQuoteRequest request,
            List<QuoteLineItem> lineItems) {
        UUID actor = actorId();
        Instant now = Instant.now();

        Opportunity opportunity = opportunityService.requireOpportunity(opportunityId);
        if (opportunity.isTerminal()) {
            throw new ValidationException("Cannot quote a terminal opportunity");
        }

        List<Quote> existing = quoteRepository
                .findByTenantIdAndOpportunityIdAndDeletedAtIsNullOrderByQuoteVersionDesc(
                        tenantId, opportunity.getId());
        for (Quote prior : existing) {
            if (prior.getStatus() == QuoteStatus.DRAFT || prior.getStatus() == QuoteStatus.SENT) {
                prior.supersede(actor);
                quoteRepository.save(prior);
            }
        }

        int nextVersion = quoteRepository.findMaxVersion(tenantId, opportunity.getId()) + 1;

        Quote quote = Quote.builder()
                .tenantId(tenantId)
                .opportunityId(opportunity.getId())
                .quoteVersion(nextVersion)
                .status(QuoteStatus.DRAFT)
                .currency(currency)
                .validUntil(request.getValidUntil())
                .notes(trimToNull(request.getNotes()))
                .createdAt(now)
                .updatedAt(now)
                .createdBy(actor)
                .updatedBy(actor)
                .build();

        for (QuoteLineItem lineItem : lineItems) {
            quote.addLineItem(lineItem);
        }

        Quote saved = quoteRepository.saveAndFlush(quote);
        eventPublisher.publish(QuoteCreatedEvent.of(
                tenantId.toString(),
                saved.getId().toString(),
                saved.getOpportunityId().toString(),
                saved.getStatus().name(),
                saved.getCurrency(),
                saved.getTotalAmount().toPlainString(),
                String.valueOf(saved.getQuoteVersion()),
                correlationId()));
        return mapper.toDto(saved);
    }

    @Transactional(readOnly = true)
    public QuoteDto get(UUID id) {
        return mapper.toDto(quoteRepository
                .findWithLineItemsByTenantIdAndId(requireTenantId(), id)
                .orElseThrow(() -> new NotFoundException("Quote not found: " + id)));
    }

    @Transactional(readOnly = true)
    public List<QuoteDto> listByOpportunity(UUID opportunityId) {
        opportunityService.requireOpportunity(opportunityId);
        return mapper.toQuoteDtos(
                quoteRepository.findWithLineItemsByTenantIdAndOpportunityId(
                        requireTenantId(), opportunityId));
    }

    @Transactional
    public QuoteDto send(UUID id) {
        Quote quote = requireQuote(id);
        quote.send(actorId());
        Quote saved = quoteRepository.save(quote);
        opportunityService.markQuoted(saved.getOpportunityId());
        eventPublisher.publish(QuoteSentEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getOpportunityId().toString(),
                saved.getStatus().name(),
                saved.getTotalAmount().toPlainString(),
                correlationId()));
        return mapper.toDto(saved);
    }

    @Transactional
    public QuoteDto accept(UUID id) {
        Quote quote = requireQuote(id);
        quote.accept(actorId());
        Quote saved = quoteRepository.save(quote);
        opportunityService.markWon(saved.getOpportunityId(), "quote accepted");
        eventPublisher.publish(QuoteAcceptedEvent.of(
                saved.getTenantId().toString(),
                saved.getId().toString(),
                saved.getOpportunityId().toString(),
                saved.getStatus().name(),
                saved.getTotalAmount().toPlainString(),
                correlationId()));
        return mapper.toDto(saved);
    }

    private QuoteLineItem resolveLineItem(QuoteLineItemRequest request, String defaultCurrency) {
        if (request.getOfferId() == null && request.getProductId() == null) {
            throw new ValidationException("Each line item requires productId or offerId");
        }

        if (request.getOfferId() != null) {
            CatalogOfferDto offer = catalogQuoteGateway.requireOffer(request.getOfferId());
            UUID productId = request.getProductId() != null ? request.getProductId() : offer.getProductId();
            return QuoteLineItem.of(
                    productId,
                    offer.getId(),
                    offer.getCode(),
                    offer.getName(),
                    request.getQuantity(),
                    offer.getUnitPrice(),
                    StringUtils.hasText(offer.getCurrency()) ? offer.getCurrency() : defaultCurrency);
        }

        CatalogProductDto product = catalogQuoteGateway.requireProduct(request.getProductId());
        throw new ValidationException(
                "offerId is required to price product " + product.getCode()
                        + "; create a catalog offer or pass offerId");
    }

    private Quote requireQuote(UUID id) {
        return quoteRepository.findWithLineItemsByTenantIdAndId(requireTenantId(), id)
                .orElseThrow(() -> new NotFoundException("Quote not found: " + id));
    }

    private UUID requireTenantId() {
        String raw = TenantContext.getTenantId();
        if (!StringUtils.hasText(raw)) {
            throw new ValidationException("Tenant context is required");
        }
        return UUID.fromString(raw);
    }

    private UUID actorId() {
        return parseUuidOrNull(TenantContext.getUserId());
    }

    private String correlationId() {
        return CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate);
    }

    private static UUID parseUuidOrNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
