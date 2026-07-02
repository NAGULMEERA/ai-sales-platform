package com.aisales.common.testing.flyway;

import org.flywaydb.core.Flyway;

import javax.sql.DataSource;

/**
 * Applies Flyway migrations for integration tests.
 */
public final class FlywayTestSupport {

    private FlywayTestSupport() {
    }

    public static void migrate(DataSource dataSource, String... locations) {
        Flyway.configure()
                .dataSource(dataSource)
                .locations(locations)
                .load()
                .migrate();
    }
}
