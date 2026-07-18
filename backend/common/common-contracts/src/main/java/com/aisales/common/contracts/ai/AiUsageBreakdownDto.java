package com.aisales.common.contracts.ai;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiUsageBreakdownDto {

    private String operation;
    private String provider;
    private String model;
    private long promptTokens;
    private long completionTokens;
    private long totalTokens;
    private BigDecimal estimatedCostUsd;
    private long requestCount;
}
