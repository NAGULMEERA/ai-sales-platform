package com.aisales.common.starter.config;

import org.springframework.boot.health.registry.HealthContributorRegistry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(HealthContributorRegistry.class)
public class ActuatorConfig {
    // Actuator endpoints are auto-configured by Spring Boot.
    // This configuration class enables common-starter actuator integration point.
}
