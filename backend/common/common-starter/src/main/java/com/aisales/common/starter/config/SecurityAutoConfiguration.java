package com.aisales.common.starter.config;

import com.aisales.common.security.config.ResourceServerConfig;
import com.aisales.common.security.config.SecurityConfig;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@ConditionalOnClass(SecurityConfig.class)
@Import({SecurityConfig.class, ResourceServerConfig.class})
public class SecurityAutoConfiguration {
}
