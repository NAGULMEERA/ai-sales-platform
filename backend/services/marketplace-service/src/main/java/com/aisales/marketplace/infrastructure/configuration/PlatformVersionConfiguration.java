package com.aisales.marketplace.infrastructure.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PlatformVersionProperties.class)
public class PlatformVersionConfiguration {
}
