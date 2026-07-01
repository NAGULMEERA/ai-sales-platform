package com.aisales.conversation.infrastructure;

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
class ConversationFlywayMigrationIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("pgvector/pgvector:pg16")
            .withDatabaseName("conversation_db")
            .withUsername("aisales")
            .withPassword("aisales");

    @Test
    void shouldApplyAllConversationMigrations() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .load();
        flyway.migrate();

        assertThat(flyway.info().applied()).hasSize(2);

        try (Connection conn = postgres.createConnection("");
             Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(
                     "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'conversations'")) {
            rs.next();
            assertThat(rs.getInt(1)).isEqualTo(1);
        }
    }
}
