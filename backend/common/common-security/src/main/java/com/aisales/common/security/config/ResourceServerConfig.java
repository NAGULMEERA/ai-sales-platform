package com.aisales.common.security.config;

import com.aisales.common.security.jwt.JwtRsaProperties;
import com.aisales.common.security.jwt.PlatformRsaKeyProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.util.StringUtils;

@Configuration
public class ResourceServerConfig {

    /**
     * RSA JwtDecoder for optional oauth2ResourceServer usage.
     * Prefers {@code aisales.security.jwt.jwk-set-uri}; otherwise uses the configured public key.
     */
    @Bean
    @ConditionalOnMissingBean(JwtDecoder.class)
    public JwtDecoder jwtDecoder(PlatformRsaKeyProvider keyProvider, JwtRsaProperties properties) {
        if (StringUtils.hasText(properties.getJwkSetUri())) {
            return NimbusJwtDecoder.withJwkSetUri(properties.getJwkSetUri())
                    .jwsAlgorithm(SignatureAlgorithm.RS256)
                    .build();
        }
        return NimbusJwtDecoder.withPublicKey(keyProvider.getPublicKey()).build();
    }
}
