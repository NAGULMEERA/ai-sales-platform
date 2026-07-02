package com.aisales.common.events.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan(basePackages = "com.aisales.common.events.kafka")
public class EventsCoreAutoConfiguration {
}
