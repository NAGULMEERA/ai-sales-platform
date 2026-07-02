package com.aisales.common.observability.metrics;

import com.aisales.common.observability.config.ObservabilityProperties;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.boot.actuate.autoconfigure.metrics.MeterRegistryCustomizer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ObservabilityProperties.class)
public class MetricsConfig {

    @Bean
    public MeterRegistryCustomizer<MeterRegistry> metricsCommonTags(
            ObservabilityProperties observabilityProperties,
            @org.springframework.beans.factory.annotation.Value("${spring.application.name:unknown}") String appName) {
        return registry -> registry.config().commonTags(
                "application", appName,
                "service", appName,
                "environment", observabilityProperties.getEnvironment());
    }
}
