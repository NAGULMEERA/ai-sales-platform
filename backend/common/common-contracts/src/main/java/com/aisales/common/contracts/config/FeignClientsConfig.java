package com.aisales.common.contracts.config;

import com.aisales.common.exception.handler.FeignErrorDecoder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableFeignClients(basePackages = "com.aisales.common.contracts.client")
public class FeignClientsConfig {

    @Bean
    public FeignErrorDecoder feignErrorDecoder(ObjectMapper objectMapper) {
        return new FeignErrorDecoder(objectMapper);
    }
}
