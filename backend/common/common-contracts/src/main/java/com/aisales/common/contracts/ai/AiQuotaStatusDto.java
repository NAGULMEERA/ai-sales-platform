package com.aisales.common.contracts.ai;

import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiQuotaStatusDto {

    private UUID tenantId;
    private boolean quotaEnforcementEnabled;
    private boolean tenantOverride;
    private String planCode;
    private String limitSource;

    private long dailyTokenLimit;
    private long dailyExecuteTokenLimit;
    private long dailyEmbedTokenLimit;

    private long usedTotalTokens;
    private long usedExecuteTokens;
    private long usedEmbedTokens;

    private long remainingTotalTokens;
    private long remainingExecuteTokens;
    private long remainingEmbedTokens;
}
