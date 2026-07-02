package com.aisales.common.testing.containers;

import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Shared Testcontainers factories for platform integration tests.
 */
public final class PlatformTestcontainers {

    private static final DockerImageName POSTGRES_IMAGE = DockerImageName.parse("postgres:16-alpine");
    private static final DockerImageName KAFKA_IMAGE = DockerImageName.parse("confluentinc/cp-kafka:7.6.1");

    private PlatformTestcontainers() {
    }

    public static org.testcontainers.containers.PostgreSQLContainer<?> postgres(String databaseName) {
        return new org.testcontainers.containers.PostgreSQLContainer<>(POSTGRES_IMAGE)
                .withDatabaseName(databaseName)
                .withUsername("aisales")
                .withPassword("aisales");
    }

    public static KafkaContainer kafka() {
        return new KafkaContainer(KAFKA_IMAGE);
    }
}
