package com.aisales.ai.infrastructure.configuration;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AiQuotaProperties.class)
public class AiQuotaConfiguration {

    @Bean
    Clock utcClock() {
        return Clock.systemUTC();
    }
}
