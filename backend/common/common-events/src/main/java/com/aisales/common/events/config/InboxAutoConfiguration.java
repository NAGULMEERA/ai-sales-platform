package com.aisales.common.events.config;

import com.aisales.common.events.inbox.DeadLetterMessage;
import com.aisales.common.events.inbox.ProcessedEvent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@Configuration
@ConditionalOnProperty(name = "aisales.events.inbox.enabled", havingValue = "true", matchIfMissing = true)
@EntityScan(basePackageClasses = {ProcessedEvent.class, DeadLetterMessage.class})
@EnableJpaRepositories(basePackages = "com.aisales.common.events.inbox")
@ComponentScan(basePackages = {
        "com.aisales.common.events.inbox",
        "com.aisales.common.events.consumer"
})
public class InboxAutoConfiguration {
}
