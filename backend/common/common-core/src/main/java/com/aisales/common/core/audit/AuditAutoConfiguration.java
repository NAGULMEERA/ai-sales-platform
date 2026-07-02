package com.aisales.common.core.audit;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;

@AutoConfiguration
@EnableConfigurationProperties(AuditProperties.class)
public class AuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditRecorder.class)
    @ConditionalOnProperty(name = "aisales.audit.logging-enabled", havingValue = "true", matchIfMissing = true)
    public AuditRecorder loggingAuditRecorder() {
        return new LoggingAuditRecorder();
    }
}
