package com.aisales.notification.infrastructure.configuration;

import com.aisales.common.core.constant.SecurityConstants;
import com.aisales.common.security.filter.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * notification-service is an internal-only service (Rule 04: "Internal APIs are consumed only by
 * microservices... never expose Internal APIs publicly") - it previously permitted every request
 * unauthenticated. This now requires a valid platform JWT for every request, and specifically the
 * {@code SERVICE} role for the notification-sending endpoints, so a leaked end-user access token
 * cannot be replayed to trigger arbitrary transactional email sends. See
 * {@code InternalServiceTokenProvider} in identity-service for how that token is minted for the
 * one caller that bypasses the gateway today.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class NotificationSecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain notificationSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SecurityConstants.PUBLIC_PATHS).permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/api/v1/notifications/**").hasRole("SERVICE")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
