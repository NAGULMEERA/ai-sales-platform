package com.aisales.common.contracts.ai;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageSummaryDto {

    private UUID tenantId;
    private Instant periodFrom;
    private Instant periodTo;
    private long promptTokens;
    private long completionTokens;
    private long totalTokens;
    private BigDecimal estimatedCostUsd;
    private long requestCount;

    @Builder.Default
    private List<AiUsageBreakdownDto> breakdown = new ArrayList<>();
}
