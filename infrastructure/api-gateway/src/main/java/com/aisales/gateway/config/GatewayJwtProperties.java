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

    /**
     * X.509 PEM content or Spring resource location for RS256 verification.
     * Local default is the committed dev public key; production injects PEM via env/secret.
     */
    private String publicKeyLocation = "classpath:jwt/local-public.pem";
}
