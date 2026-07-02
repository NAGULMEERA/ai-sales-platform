package com.aisales.lead.infrastructure;

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
class LeadFlywayMigrationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("lead_db")
            .withUsername("aisales")
            .withPassword("aisales");

    @Test
    void shouldApplyAllLeadMigrations() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        assertThat(flyway.info().applied()).hasSize(7);

        try (Connection conn = postgres.createConnection("");
             Statement st = conn.createStatement()) {
            assertTableExists(st, "leads");
            assertTableExists(st, "outbox_events");
            assertTableExists(st, "processed_events");
            assertTableExists(st, "dead_letter");
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
}
