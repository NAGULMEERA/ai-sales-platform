package com.aisales.common.events.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "aisales.events.inbox.enabled", havingValue = "true", matchIfMissing = true)
@ComponentScan(basePackages = {
        "com.aisales.common.events.inbox",
        "com.aisales.common.events.consumer"
})
public class InboxAutoConfiguration {
}
