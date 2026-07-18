package com.aisales.integration.infrastructure.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
    MetaLeadAdsProperties.class,
    IntegrationServiceAuthProperties.class,
    VoiceProperties.class
})
public class IntegrationConfiguration {
}
