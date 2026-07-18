package com.aisales.common.contracts.client;

import com.aisales.common.contracts.deal.AssignOpportunityRequest;
import com.aisales.common.contracts.deal.CreateOpportunityRequest;
import com.aisales.common.contracts.deal.CreateQuoteRequest;
import com.aisales.common.contracts.deal.OpportunityDto;
import com.aisales.common.contracts.deal.OpportunityStatus;
import com.aisales.common.contracts.deal.QuoteDto;
import com.aisales.common.contracts.deal.UpdateOpportunityRequest;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.core.dto.PageResponse;
import java.util.UUID;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "deal-service",
        path = "/api/v1",
        url = "${aisales.clients.deal-service.url:}")
public interface DealServiceClient {

    @PostMapping("/opportunities")
    ApiResponse<OpportunityDto> createOpportunity(@RequestBody CreateOpportunityRequest request);

    @GetMapping("/opportunities/{id}")
    ApiResponse<OpportunityDto> getOpportunity(@PathVariable UUID id);

    @GetMapping("/opportunities")
    ApiResponse<PageResponse<OpportunityDto>> listOpportunities(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) OpportunityStatus status,
            @RequestParam(required = false) UUID customerId,
            @RequestParam(required = false) UUID leadId);

    @PutMapping("/opportunities/{id}")
    ApiResponse<OpportunityDto> updateOpportunity(
            @PathVariable UUID id, @RequestBody UpdateOpportunityRequest request);

    @PostMapping("/opportunities/{id}/assign")
    ApiResponse<OpportunityDto> assignOpportunity(
            @PathVariable UUID id, @RequestBody AssignOpportunityRequest request);

    @PostMapping("/quotes")
    ApiResponse<QuoteDto> createQuote(@RequestBody CreateQuoteRequest request);

    @GetMapping("/quotes/{id}")
    ApiResponse<QuoteDto> getQuote(@PathVariable UUID id);

    @GetMapping("/opportunities/{opportunityId}/quotes")
    ApiResponse<java.util.List<QuoteDto>> listQuotes(@PathVariable UUID opportunityId);

    @PostMapping("/quotes/{id}/send")
    ApiResponse<QuoteDto> sendQuote(@PathVariable UUID id);

    @PostMapping("/quotes/{id}/accept")
    ApiResponse<QuoteDto> acceptQuote(@PathVariable UUID id);
}
