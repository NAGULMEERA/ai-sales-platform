package com.aisales.ai.infrastructure.configuration;

import java.util.LinkedHashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "aisales.ai.quota")
public class AiQuotaProperties {

    /** When false, quota checks are skipped (usage is still recorded). */
    private boolean enabled = true;

    /**
     * Default overall daily total-token budget when no {@code tenant_ai_quota} row exists.
     * {@code 0} or negative means unlimited for the overall path.
     */
    private long dailyTokenLimit = 500_000L;

    /**
     * Default EXECUTE daily budget. {@code 0} = no separate EXECUTE cap.
     */
    private long dailyExecuteTokenLimit = 400_000L;

    /**
     * Default EMBED daily budget. {@code 0} = no separate EMBED cap.
     */
    private long dailyEmbedTokenLimit = 100_000L;

    /**
     * Tokens reserved against the daily EXECUTE budget before an LLM call.
     * Released after usage is recorded (or on failure).
     */
    private long reserveExecuteTokens = 4_096L;

    /**
     * Tokens reserved against the daily EMBED budget before an embedding batch.
     */
    private long reserveEmbedTokens = 8_192L;

    /**
     * Plan-linked packages applied on {@code SubscriptionPlanChanged} / {@code TenantCreated}.
     * Keys are plan codes (FREE, PREMIUM).
     */
    private Map<String, PlanPackage> plans = defaultPlans();

    @Data
    public static class PlanPackage {
        private long dailyTokenLimit = 100_000L;
        private long dailyExecuteTokenLimit = 80_000L;
        private long dailyEmbedTokenLimit = 20_000L;
    }

    private static Map<String, PlanPackage> defaultPlans() {
        Map<String, PlanPackage> plans = new LinkedHashMap<>();
        PlanPackage free = new PlanPackage();
        free.setDailyTokenLimit(100_000L);
        free.setDailyExecuteTokenLimit(80_000L);
        free.setDailyEmbedTokenLimit(20_000L);
        plans.put("FREE", free);

        PlanPackage premium = new PlanPackage();
        premium.setDailyTokenLimit(2_000_000L);
        premium.setDailyExecuteTokenLimit(1_600_000L);
        premium.setDailyEmbedTokenLimit(400_000L);
        plans.put("PREMIUM", premium);
        return plans;
    }
}
