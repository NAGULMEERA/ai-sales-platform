package com.aisales.identity.integration;

import com.aisales.common.testing.containers.PlatformTestcontainers;
import com.aisales.identity.IdentityServiceApplication;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.mockito.Mockito.mock;

@SpringBootTest(classes = IdentityServiceApplication.class)
@ActiveProfiles("test")
@Import(IdentityIntegrationTestBase.TestKafkaConfig.class)
@Testcontainers(disabledWithoutDocker = true)
public abstract class IdentityIntegrationTestBase {

    @Container
    protected static final PostgreSQLContainer<?> POSTGRES = PlatformTestcontainers.postgres("identity_it");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @BeforeEach
    void waitForPostgres() {
        if (!POSTGRES.isRunning()) {
            POSTGRES.start();
        }
    }

    @TestConfiguration
    static class TestKafkaConfig {
        @Bean
        @Primary
        KafkaTemplate<String, String> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }
    }
}
