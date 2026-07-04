package com.aisales.common.starter.validation;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class StartupValidationRunner implements ApplicationRunner {

    private final StartupValidationProperties properties;
    private final StartupValidationState state;
    private final Environment environment;
    private final ApplicationContext applicationContext;
    private final ObjectProvider<DataSource> dataSource;
    private final ObjectProvider<RedisConnectionFactory> redisConnectionFactory;

    @Override
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            state.markValidated();
            return;
        }
        List<String> failures = new ArrayList<>();
        validateRequiredConfig(failures);
        validateDatabase(failures);
        validateFlyway(failures);
        validateRedis(failures);
        validateKafka(failures);
        validateExternalDependencies(failures);

        if (!failures.isEmpty()) {
            state.markFailed(failures);
            throw new IllegalStateException("Startup validation failed: " + String.join("; ", failures));
        }
        state.markValidated();
        log.info("startup_validation_completed");
    }

    private void validateRequiredConfig(List<String> failures) {
        for (String key : properties.getRequiredConfigValues()) {
            if (!StringUtils.hasText(environment.getProperty(key))) {
                failures.add("Missing required configuration value: " + key);
            }
        }
    }

    private void validateDatabase(List<String> failures) {
        DataSource configuredDataSource = dataSource.getIfAvailable();
        if (configuredDataSource == null) {
            return;
        }
        try (Connection connection = configuredDataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            for (String table : properties.getRequiredTables()) {
                if (!tableExists(metaData, table)) {
                    failures.add("Missing required table: " + table);
                }
            }
        } catch (Exception ex) {
            failures.add("Database startup validation failed: " + ex.getMessage());
        }
    }

    private boolean tableExists(DatabaseMetaData metaData, String table) throws Exception {
        try (ResultSet rs = metaData.getTables(null, null, table, new String[]{"TABLE"})) {
            if (rs.next()) {
                return true;
            }
        }
        try (ResultSet rs = metaData.getTables(null, "public", table, new String[]{"TABLE"})) {
            return rs.next();
        }
    }

    private void validateRedis(List<String> failures) {
        if (!properties.isValidateRedis()) {
            return;
        }
        RedisConnectionFactory connectionFactory = redisConnectionFactory.getIfAvailable();
        if (connectionFactory == null) {
            return;
        }
        try (var connection = connectionFactory.getConnection()) {
            connection.ping();
        } catch (Exception ex) {
            failures.add("Redis startup validation failed: " + ex.getMessage());
        }
    }

    private void validateFlyway(List<String> failures) {
        if (!properties.isValidateFlyway()) {
            return;
        }
        Class<?> flywayClass = flywayClass();
        if (flywayClass == null) {
            return;
        }
        for (String beanName : applicationContext.getBeanNamesForType(flywayClass, false, false)) {
            try {
                Object configuredFlyway = applicationContext.getBean(beanName);
                Object infoResult = configuredFlyway.getClass().getMethod("info").invoke(configuredFlyway);
                Object[] pending = (Object[]) infoResult.getClass().getMethod("pending").invoke(infoResult);
                if (pending.length > 0) {
                    failures.add("Pending Flyway migrations: " + pending.length);
                }
            } catch (Exception ex) {
                failures.add("Flyway startup validation failed: " + ex.getMessage());
            }
        }
    }

    private Class<?> flywayClass() {
        try {
            return Class.forName("org.flywaydb.core.Flyway");
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    private void validateExternalDependencies(List<String> failures) {
        if (properties.getRequiredExternalHealthUrls().isEmpty()) {
            return;
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        for (String url : properties.getRequiredExternalHealthUrls()) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .timeout(Duration.ofSeconds(3))
                        .GET()
                        .build();
                HttpResponse<Void> response = client.send(request, HttpResponse.BodyHandlers.discarding());
                if (response.statusCode() >= 500) {
                    failures.add("External dependency unhealthy (" + response.statusCode() + "): " + url);
                }
            } catch (Exception ex) {
                failures.add("External dependency unreachable: " + url + " (" + ex.getMessage() + ")");
            }
        }
    }

    private void validateKafka(List<String> failures) {
        if (!properties.isValidateKafka()) {
            return;
        }
        Class<?> kafkaAdminClass = kafkaAdminClass();
        if (kafkaAdminClass == null) {
            return;
        }
        for (String beanName : applicationContext.getBeanNamesForType(kafkaAdminClass, false, false)) {
            try {
                Object kafkaAdmin = applicationContext.getBean(beanName);
                Method initialize = kafkaAdmin.getClass().getMethod("initialize");
                initialize.invoke(kafkaAdmin);
            } catch (Exception ex) {
                failures.add("Kafka startup validation failed for bean " + beanName + ": " + rootMessage(ex));
            }
        }
    }

    private Class<?> kafkaAdminClass() {
        try {
            return Class.forName("org.springframework.kafka.core.KafkaAdmin");
        } catch (ClassNotFoundException ex) {
            return null;
        }
    }

    private static String rootMessage(Exception ex) {
        Throwable cause = ex;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }
}
