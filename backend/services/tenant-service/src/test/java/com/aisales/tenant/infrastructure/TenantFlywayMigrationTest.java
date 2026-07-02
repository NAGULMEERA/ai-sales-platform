package com.aisales.tenant.infrastructure;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
class TenantFlywayMigrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("tenant_db")
            .withUsername("aisales")
            .withPassword("aisales");

    @Test
    void shouldApplyAllTenantMigrations() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        assertThat(flyway.info().applied()).hasSize(9);

        try (Connection conn = postgres.createConnection("");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT table_name
                     FROM information_schema.tables
                     WHERE table_schema = 'public'
                       AND table_name IN (
                           'outbox_events', 'idempotency_keys', 'tenant_audit_log',
                           'processed_events', 'dead_letter'
                       )
                     """)) {
            int tableCount = 0;
            while (rs.next()) {
                tableCount++;
            }
            assertThat(tableCount).isEqualTo(5);
        }

        try (Connection conn = postgres.createConnection("");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("""
                     SELECT column_name
                     FROM information_schema.columns
                     WHERE table_schema = 'public'
                       AND table_name = 'tenants'
                       AND column_name IN ('tenant_code', 'industry', 'subscription_plan', 'deleted', 'version')
                     """)) {
            int count = 0;
            while (rs.next()) {
                count++;
            }
            assertThat(count).isEqualTo(5);
        }
    }
}
