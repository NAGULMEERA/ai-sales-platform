package com.aisales.common.observability.metrics;

import com.aisales.common.observability.config.ObservabilityProperties;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiRequestMetrics {

    private static final String AI_REQUEST_DURATION = "ai_request_duration";

    private final PlatformMetrics platformMetrics;
    private final ObservabilityProperties observabilityProperties;

    public Timer.Sample startTimer() {
        return platformMetrics.startTimer();
    }

    public void recordRequest(Timer.Sample sample, String tenantId, String provider, String operation,
                              String outcome, long elapsedMs) {
        platformMetrics.incrementBusinessMetric(MetricNames.AI_REQUEST, tenantId,
                "provider", provider, "operation", operation, "outcome", outcome);
        platformMetrics.recordTimer(sample, AI_REQUEST_DURATION,
                "provider", provider, "operation", operation, "outcome", outcome);
        if (elapsedMs >= observabilityProperties.getSlowOutboundCallThreshold().toMillis()) {
            log.warn("slow_ai_request provider={} operation={} outcome={} duration_ms={}",
                    provider, operation, outcome, elapsedMs);
            platformMetrics.incrementBusinessMetric(MetricNames.AI_SLOW_REQUEST, tenantId,
                    "provider", provider, "operation", operation, "outcome", outcome);
        }
    }

    public void recordTokens(String tenantId, String provider, String operation, double tokens) {
        if (tokens > 0) {
            platformMetrics.incrementBusinessMetric(MetricNames.AI_TOKENS, tokens, tenantId,
                    "provider", provider, "operation", operation);
        }
    }
}
