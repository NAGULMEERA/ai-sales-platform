package com.aisales.tenant.infrastructure.persistence;

import com.aisales.common.contracts.tenant.SubscriptionPlan;
import com.aisales.common.contracts.tenant.TenantIndustry;
import com.aisales.common.contracts.tenant.TenantStatus;
import com.aisales.common.core.config.CoreConfig;
import com.aisales.tenant.domain.entity.Tenant;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(CoreConfig.class)
class TenantJpaRepositoryTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tenant_repo")
            .withUsername("aisales")
            .withPassword("aisales");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private TenantJpaRepository tenantJpaRepository;

    @BeforeAll
    static void migrate() {
        Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load()
                .migrate();
    }

    @Test
    void shouldPersistAndFindActiveTenant() {
        Tenant tenant = Tenant.builder()
                .tenantCode("ACME_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .slug("acme-" + UUID.randomUUID().toString().substring(0, 8))
                .name("Acme Corp")
                .industry(TenantIndustry.EDUCATION)
                .subscriptionPlan(SubscriptionPlan.PREMIUM)
                .status(TenantStatus.ACTIVE)
                .timezone("Asia/Kolkata")
                .language("en")
                .deleted(false)
                .build();

        Tenant saved = tenantJpaRepository.save(tenant);

        assertThat(tenantJpaRepository.findByIdAndDeletedFalse(saved.getId())).isPresent();
        assertThat(tenantJpaRepository.existsBySlug(saved.getSlug())).isTrue();
    }

    @Test
    void shouldExcludeSoftDeletedTenantsFromActiveQueries() {
        Tenant tenant = Tenant.builder()
                .tenantCode("DEL_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase())
                .slug("deleted-" + UUID.randomUUID().toString().substring(0, 8))
                .name("Deleted Corp")
                .industry(TenantIndustry.HEALTHCARE)
                .subscriptionPlan(SubscriptionPlan.FREE)
                .status(TenantStatus.SUSPENDED)
                .timezone("UTC")
                .language("en")
                .deleted(true)
                .build();

        Tenant saved = tenantJpaRepository.save(tenant);

        assertThat(tenantJpaRepository.findByIdAndDeletedFalse(saved.getId())).isEmpty();
        assertThat(tenantJpaRepository.findAllByDeletedFalse()).noneMatch(t -> t.getId().equals(saved.getId()));
    }
}
