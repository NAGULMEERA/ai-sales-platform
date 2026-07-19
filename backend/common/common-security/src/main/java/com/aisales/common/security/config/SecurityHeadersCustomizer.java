package com.aisales.common.security.config;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;

/**
 * Shared HTTP security headers for servlet-based services.
 * Reuse from service-specific {@code SecurityFilterChain} beans that replace platform defaults.
 */
public final class SecurityHeadersCustomizer {

    private SecurityHeadersCustomizer() {
    }

    public static void apply(HttpSecurity http) throws Exception {
        http.headers(headers -> headers
                .contentTypeOptions(Customizer.withDefaults())
                .frameOptions(frame -> frame.deny())
                .httpStrictTransportSecurity(hsts -> hsts
                        .includeSubDomains(true)
                        .preload(true)
                        .maxAgeInSeconds(31536000))
                .referrerPolicy(referrer -> referrer.policy(
                        ReferrerPolicyHeaderWriter.ReferrerPolicy.NO_REFERRER))
                .permissionsPolicy(permissions -> permissions.policy(
                        "camera=(), microphone=(), geolocation=(), payment=()")));
    }
}
