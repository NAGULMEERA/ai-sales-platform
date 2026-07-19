package com.aisales.customer.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(CustomerMatchingProperties.class)
public class CustomerServiceConfiguration {
}
