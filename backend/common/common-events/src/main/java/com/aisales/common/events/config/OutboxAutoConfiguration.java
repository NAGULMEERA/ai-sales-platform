package com.aisales.common.events.config;

import com.aisales.common.events.outbox.OutboxEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "aisales.events.outbox.enabled", havingValue = "true")
@EntityScan(basePackageClasses = OutboxEvent.class)
@EnableJpaRepositories(basePackages = "com.aisales.common.events.outbox")
@ComponentScan(basePackages = "com.aisales.common.events.outbox")
public class OutboxAutoConfiguration {
}
