package com.aisales.identity.integration;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
class IdentityFlywayMigrationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("identity_db")
            .withUsername("aisales")
            .withPassword("aisales");

    @Test
    void shouldApplyAllIdentityMigrations() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        assertThat(flyway.info().applied()).hasSize(11);

        try (Connection conn = postgres.createConnection("");
             Statement st = conn.createStatement()) {
            assertTableExists(st, "users");
            assertTableExists(st, "tenants");
            assertTableExists(st, "permissions");
            assertTableExists(st, "tenant_subscriptions");
            assertTableExists(st, "audit_logs");
            assertTableExists(st, "processed_events");
            assertTableExists(st, "dead_letter");
            assertDeadLetterRetryColumnsExist(st);
        }
    }

    private static void assertTableExists(Statement st, String table) throws Exception {
        try (ResultSet rs = st.executeQuery(
                "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = '"
                        + table + "'")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }

    private static void assertDeadLetterRetryColumnsExist(Statement st) throws Exception {
        try (ResultSet rs = st.executeQuery("""
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_schema = 'public'
                  AND table_name = 'dead_letter'
                  AND column_name IN ('error_class', 'retry_count', 'last_attempt_at')
                """)) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(3);
        }
    }
}
