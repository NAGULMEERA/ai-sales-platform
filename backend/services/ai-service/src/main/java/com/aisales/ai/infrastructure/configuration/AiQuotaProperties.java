package com.aisales.ai.infrastructure.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aisales.ai.quota")
public class AiQuotaProperties {

    /** When false, quota checks are skipped (usage is still recorded). */
    private boolean enabled = true;

    /**
     * Default daily total-token budget per tenant when no {@code tenant_ai_quota} row exists.
     * {@code 0} or negative means unlimited for the default path.
     */
    private long dailyTokenLimit = 500_000L;
}
