package com.aisales.workflow.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.common.testing.containers.PlatformTestcontainers;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
class WorkflowFlywayMigrationIT {

    @Container
    static PostgreSQLContainer<?> postgres = PlatformTestcontainers.postgres("workflow_db");

    @Test
    void shouldApplyWorkflowMigrationsAndCreateCoreTables() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        assertThat(flyway.info().applied()).isNotEmpty();

        try (Connection conn = postgres.createConnection("");
                Statement st = conn.createStatement()) {
            assertTableExists(st, "outbox_events");
            assertTableExists(st, "processed_events");
            assertTableExists(st, "dead_letter");
            assertTableExists(st, "workflow_rule");
            assertTableExists(st, "workflow_automation_execution");
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
