package com.aisales.common.starter.config;

import com.aisales.common.starter.validation.StartupValidationProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.ComponentScan;

@AutoConfiguration
@EnableConfigurationProperties(StartupValidationProperties.class)
@ComponentScan(basePackages = "com.aisales.common.starter.validation")
public class StartupValidationAutoConfiguration {
}
