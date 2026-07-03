package com.aisales.common.events.config;

import com.aisales.common.events.schema.EventSchemaRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class EventSchemaAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public EventSchemaRegistry eventSchemaRegistry(ObjectMapper objectMapper) {
        return new EventSchemaRegistry(objectMapper);
    }
}
