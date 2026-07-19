package com.aisales.common.contracts.ai;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpsertAiQuotaRequest {

    /**
     * Overall daily token budget (EXECUTE + EMBED). Must be &gt; 0.
     */
    @NotNull
    @Min(1)
    private Long dailyTokenLimit;

    /**
     * Optional EXECUTE-only daily budget. Null keeps existing / default package value.
     * {@code 0} means no separate EXECUTE cap (overall still applies).
     */
    @Min(0)
    private Long dailyExecuteTokenLimit;

    /**
     * Optional EMBED-only daily budget. Null keeps existing / default package value.
     * {@code 0} means no separate EMBED cap (overall still applies).
     */
    @Min(0)
    private Long dailyEmbedTokenLimit;

    @Builder.Default
    private Boolean enabled = true;

    /** Optional plan label stored for observability (FREE, PREMIUM, CUSTOM). */
    private String planCode;
}
