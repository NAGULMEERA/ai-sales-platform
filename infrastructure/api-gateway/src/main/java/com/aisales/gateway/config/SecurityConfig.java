package com.aisales.gateway.config;

import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.header.ReferrerPolicyServerHttpHeadersWriter;
import org.springframework.security.web.server.header.XFrameOptionsServerHttpHeadersWriter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

/**
 * Gateway auth is enforced by {@code JwtAuthenticationFilter} (GlobalFilter), which
 * validates JWTs and returns 401. Spring Security stays permit-all so it does not
 * require a ReactiveSecurityContext that the JWT filter never populates.
 * Security headers and optional CORS allowlist are applied here at the edge.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${aisales.security.cors.allowed-origins:}")
    private List<String> corsAllowedOrigins;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchanges -> exchanges.anyExchange().permitAll())
                .headers(headers -> headers
                        .frameOptions(frame -> frame.mode(XFrameOptionsServerHttpHeadersWriter.Mode.DENY))
                        .hsts(hsts -> hsts.includeSubdomains(true).maxAge(Duration.ofDays(365)))
                        .referrerPolicy(referrer -> referrer.policy(
                                ReferrerPolicyServerHttpHeadersWriter.ReferrerPolicy.NO_REFERRER)));
        if (corsAllowedOrigins != null && !corsAllowedOrigins.isEmpty()) {
            http.cors(cors -> cors.configurationSource(corsConfigurationSource()));
        } else {
            http.cors(ServerHttpSecurity.CorsSpec::disable);
        }
        return http.build();
    }

    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(corsAllowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of(
                "Authorization", "Content-Type", "X-Correlation-Id", "X-Tenant-Id", "Idempotency-Key"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
