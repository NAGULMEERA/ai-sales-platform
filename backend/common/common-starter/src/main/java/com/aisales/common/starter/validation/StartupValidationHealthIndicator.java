package com.aisales.common.starter.validation;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component("startupValidation")
@RequiredArgsConstructor
public class StartupValidationHealthIndicator implements HealthIndicator {

    private final StartupValidationProperties properties;
    private final StartupValidationState state;

    @Override
    public Health health() {
        if (!properties.isEnabled()) {
            return Health.up().withDetail("enabled", false).build();
        }
        if (state.isValidated()) {
            return Health.up().withDetail("validated", true).build();
        }
        return Health.down()
                .withDetail("validated", false)
                .withDetail("failures", state.getFailures())
                .build();
    }
}
