package com.aisales.deal.api.controller;

import com.aisales.common.contracts.deal.CreateQuoteRequest;
import com.aisales.common.contracts.deal.QuoteDto;
import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.deal.application.service.QuoteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Quotes", description = "Commercial quotes linked to opportunities")
public class QuoteController {

    private final QuoteService quoteService;

    @PostMapping("/api/v1/quotes")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a quote (snapshots catalog offer prices)")
    public ApiResponse<QuoteDto> create(
            @Valid @RequestBody CreateQuoteRequest request,
            @RequestHeader(value = ApiConstants.IDEMPOTENCY_KEY_HEADER, required = false) String idempotencyKey) {
        return ApiResponse.ok(quoteService.create(request, idempotencyKey));
    }

    @GetMapping("/api/v1/quotes/{id}")
    @Operation(summary = "Get a quote")
    public ApiResponse<QuoteDto> get(@PathVariable UUID id) {
        return ApiResponse.ok(quoteService.get(id));
    }

    @GetMapping("/api/v1/opportunities/{opportunityId}/quotes")
    @Operation(summary = "List quotes for an opportunity")
    public ApiResponse<List<QuoteDto>> listByOpportunity(@PathVariable UUID opportunityId) {
        return ApiResponse.ok(quoteService.listByOpportunity(opportunityId));
    }

    @PostMapping("/api/v1/quotes/{id}/send")
    @Operation(summary = "Send a draft quote")
    public ApiResponse<QuoteDto> send(@PathVariable UUID id) {
        return ApiResponse.ok(quoteService.send(id));
    }

    @PostMapping("/api/v1/quotes/{id}/accept")
    @Operation(summary = "Accept a sent quote (marks opportunity WON)")
    public ApiResponse<QuoteDto> accept(@PathVariable UUID id) {
        return ApiResponse.ok(quoteService.accept(id));
    }
}
