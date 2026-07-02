package com.aisales.common.observability.tracing;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * OpenTelemetry tracing is auto-configured by Spring Boot via Micrometer Tracing
 * ({@code micrometer-tracing-bridge-otel} + {@code opentelemetry-exporter-otlp}).
 *
 * <p>Configure with:
 * <pre>
 * management.tracing.sampling.probability: 1.0
 * management.otlp.tracing.endpoint: http://localhost:4318/v1/traces
 * </pre>
 */
@Configuration
@ConditionalOnProperty(name = "aisales.tracing.enabled", havingValue = "true", matchIfMissing = true)
public class TracingConfig {
}
