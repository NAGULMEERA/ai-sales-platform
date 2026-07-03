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
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
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
        validateRedis(failures);
        validateKafka(failures);

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
