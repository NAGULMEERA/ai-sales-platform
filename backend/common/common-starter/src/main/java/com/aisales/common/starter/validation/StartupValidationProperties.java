package com.aisales.common.starter.validation;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ConfigurationProperties(prefix = "aisales.startup-validation")
public class StartupValidationProperties {

    /** Fail startup when validation fails. */
    private boolean enabled = true;

    /** Configuration keys that must resolve to non-blank values. */
    private List<String> requiredConfigValues = new ArrayList<>();

    /** Database tables that must exist when a DataSource is present. */
    private List<String> requiredTables = new ArrayList<>();

    /** Validate Redis connectivity when Redis is configured. */
    private boolean validateRedis = true;

    /** Validate Flyway has no pending migrations when Flyway is configured. */
    private boolean validateFlyway = true;

    /** Optional actuator/health URLs that must respond before accepting traffic. */
    private List<String> requiredExternalHealthUrls = new ArrayList<>();

    /** Validate Kafka admin connectivity when KafkaAdmin is configured. */
    private boolean validateKafka = true;
}
