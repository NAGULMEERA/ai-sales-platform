package com.aisales.gateway.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aisales.security.jwt")
public class GatewayJwtProperties {

    private String secret = "aisales-default-jwt-secret-key-change-in-production";
}
