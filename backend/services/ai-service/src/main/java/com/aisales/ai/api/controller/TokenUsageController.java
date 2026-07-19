package com.aisales.ai.api.controller;

import com.aisales.ai.application.service.TokenUsageQueryService;
import com.aisales.common.contracts.ai.AiUsageSummaryDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.security.annotation.PreAuthorizeTenant;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Token Usage", description = "AI usage ledger read APIs (Billing consumes summaries)")
public class TokenUsageController {

    private final TokenUsageQueryService tokenUsageQueryService;

    @GetMapping("/api/v1/token-usage/summary")
    @PreAuthorizeTenant
    @Operation(summary = "Tenant AI usage summary for a period (ledger estimates, not invoices)")
    public ApiResponse<AiUsageSummaryDto> summarize(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ApiResponse.ok(tokenUsageQueryService.summarize(from, to));
    }
}
