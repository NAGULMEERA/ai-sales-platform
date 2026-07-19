package com.aisales.deal.application.mapper;

import com.aisales.common.contracts.deal.OpportunityDto;
import com.aisales.common.contracts.deal.QuoteDto;
import com.aisales.common.contracts.deal.QuoteLineItemDto;
import com.aisales.deal.domain.entity.Opportunity;
import com.aisales.deal.domain.entity.Quote;
import com.aisales.deal.domain.entity.QuoteLineItem;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class DealMapper {

    public OpportunityDto toDto(Opportunity opportunity) {
        return OpportunityDto.builder()
                .id(opportunity.getId())
                .tenantId(opportunity.getTenantId())
                .organizationId(opportunity.getOrganizationId())
                .leadId(opportunity.getLeadId())
                .customerId(opportunity.getCustomerId())
                .name(opportunity.getName())
                .amount(opportunity.getAmount())
                .currency(opportunity.getCurrency())
                .status(opportunity.getStatus())
                .probability(opportunity.getProbability())
                .score(opportunity.getScore())
                .expectedCloseDate(opportunity.getExpectedCloseDate())
                .assignedTo(opportunity.getAssignedTo())
                .catalogProductId(opportunity.getCatalogProductId())
                .catalogOfferId(opportunity.getCatalogOfferId())
                .notes(opportunity.getNotes())
                .closeReason(opportunity.getCloseReason())
                .createdAt(opportunity.getCreatedAt())
                .updatedAt(opportunity.getUpdatedAt())
                .version(opportunity.getVersion())
                .build();
    }

    public QuoteDto toDto(Quote quote) {
        return QuoteDto.builder()
                .id(quote.getId())
                .tenantId(quote.getTenantId())
                .opportunityId(quote.getOpportunityId())
                .quoteVersion(quote.getQuoteVersion())
                .status(quote.getStatus())
                .currency(quote.getCurrency())
                .totalAmount(quote.getTotalAmount())
                .validUntil(quote.getValidUntil())
                .notes(quote.getNotes())
                .lineItems(quote.getLineItems().stream().map(this::toLineDto).toList())
                .createdAt(quote.getCreatedAt())
                .updatedAt(quote.getUpdatedAt())
                .version(quote.getVersion())
                .build();
    }

    public QuoteLineItemDto toLineDto(QuoteLineItem item) {
        return QuoteLineItemDto.builder()
                .id(item.getId())
                .productId(item.getProductId())
                .offerId(item.getOfferId())
                .code(item.getCode())
                .name(item.getName())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .currency(item.getCurrency())
                .lineTotal(item.getLineTotal())
                .build();
    }

    public List<QuoteDto> toQuoteDtos(List<Quote> quotes) {
        return quotes.stream().map(this::toDto).toList();
    }
}
