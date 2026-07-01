package com.aisales.common.observability.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            @org.springframework.beans.factory.annotation.Value("${spring.application.name:unknown}") String appName) {
        return registry -> registry.config().commonTags("application", appName);
    }
}
