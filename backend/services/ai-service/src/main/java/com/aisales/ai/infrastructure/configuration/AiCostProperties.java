package com.aisales.ai.infrastructure.configuration;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Config-driven USD estimates for AI usage (ledger only — not billing invoices).
 */
@Data
@ConfigurationProperties(prefix = "aisales.ai.cost")
public class AiCostProperties {

    private boolean enabled = true;

    private Map<String, ModelRate> models = new HashMap<>();

    @Data
    public static class ModelRate {
        /** USD per 1k prompt tokens (LLM). */
        private BigDecimal promptPer1k = BigDecimal.ZERO;
        /** USD per 1k completion tokens (LLM). */
        private BigDecimal completionPer1k = BigDecimal.ZERO;
        /** USD per 1k embedding tokens. */
        private BigDecimal embeddingPer1k = BigDecimal.ZERO;
    }
}
