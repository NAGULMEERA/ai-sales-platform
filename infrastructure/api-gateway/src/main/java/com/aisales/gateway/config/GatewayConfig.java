package com.aisales.gateway.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.cloud.gateway.filter.ratelimit.RedisRateLimiter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GatewayRateLimitProperties.class)
public class GatewayConfig {

    private final GatewayRateLimitProperties rateLimitProperties;
    private final KeyResolver clientIpKeyResolver;
    private RedisRateLimiter authRedisRateLimiter;
    private RedisRateLimiter passwordResetRedisRateLimiter;
    private RedisRateLimiter aiExecuteRedisRateLimiter;

    public GatewayConfig(GatewayRateLimitProperties rateLimitProperties, KeyResolver clientIpKeyResolver) {
        this.rateLimitProperties = rateLimitProperties;
        this.clientIpKeyResolver = clientIpKeyResolver;
    }

    @Autowired(required = false)
    public void setRateLimiters(
            @Qualifier("authRedisRateLimiter") RedisRateLimiter authRedisRateLimiter,
            @Qualifier("passwordResetRedisRateLimiter") RedisRateLimiter passwordResetRedisRateLimiter,
            @Qualifier("aiExecuteRedisRateLimiter") RedisRateLimiter aiExecuteRedisRateLimiter) {
        this.authRedisRateLimiter = authRedisRateLimiter;
        this.passwordResetRedisRateLimiter = passwordResetRedisRateLimiter;
        this.aiExecuteRedisRateLimiter = aiExecuteRedisRateLimiter;
    }

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        RouteLocatorBuilder.Builder routes = builder.routes();

        if (rateLimitProperties.isEnabled()
                && authRedisRateLimiter != null
                && passwordResetRedisRateLimiter != null) {
            routes = routes
                    .route("identity-auth-rate-limited", r -> r
                            .path("/api/v1/auth/login", "/api/v1/auth/register", "/api/v1/auth/refresh")
                            .filters(f -> f.requestRateLimiter(config -> config
                                    .setRateLimiter(authRedisRateLimiter)
                                    .setKeyResolver(clientIpKeyResolver)
                                    .setDenyEmptyKey(false)))
                            .uri("lb://identity-service"))
                    .route("identity-auth-password-rate-limited", r -> r
                            .path(
                                    "/api/v1/auth/forgot-password",
                                    "/api/v1/auth/reset-password",
                                    "/api/v1/auth/resend-verification")
                            .filters(f -> f.requestRateLimiter(config -> config
                                    .setRateLimiter(passwordResetRedisRateLimiter)
                                    .setKeyResolver(clientIpKeyResolver)
                                    .setDenyEmptyKey(false)))
                            .uri("lb://identity-service"));
        }

        if (rateLimitProperties.isEnabled() && aiExecuteRedisRateLimiter != null) {
            // More specific than the catch-all ai-service route below.
            routes = routes.route("ai-execute-rate-limited", r -> r
                    .path("/api/v1/ai/execute")
                    .filters(f -> f.requestRateLimiter(config -> config
                            .setRateLimiter(aiExecuteRedisRateLimiter)
                            .setKeyResolver(clientIpKeyResolver)
                            .setDenyEmptyKey(false)))
                    .uri("lb://ai-service"));
        }

        return routes
                .route("identity-service", r -> r.path("/api/v1/auth/**", "/api/v1/users/**")
                        .uri("lb://identity-service"))
                .route("tenant-service", r -> r.path("/api/v1/tenants/**", "/api/v1/tenant-users/**")
                        .uri("lb://tenant-service"))
                .route("lead-service", r -> r.path("/api/v1/leads/**", "/api/v1/lead/**")
                        .uri("lb://lead-service"))
                .route("customer-service", r -> r.path("/api/v1/customers/**", "/api/v1/customer/**")
                        .uri("lb://customer-service"))
                .route("catalog-service", r -> r.path("/api/v1/catalog/**", "/api/v1/catalogs/**")
                        .uri("lb://catalog-service"))
                .route("conversation-service", r -> r.path("/api/v1/conversations/**", "/api/v1/conversation/**")
                        .uri("lb://conversation-service"))
                .route("appointment-service", r -> r.path("/api/v1/appointments/**", "/api/v1/appointment/**")
                        .uri("lb://appointment-service"))
                .route("ai-service", r -> r.path(
                                "/api/v1/ai/**",
                                "/api/v1/prompts/**",
                                "/api/v1/embeddings/**",
                                "/api/v1/token-usage/**",
                                "/api/v1/knowledge-bases/**",
                                "/api/v1/knowledge-documents/**")
                        .uri("lb://ai-service"))
                .route("workflow-service", r -> r.path("/api/v1/workflows/**", "/api/v1/workflow/**")
                        .uri("lb://workflow-service"))
                .route("notification-service", r -> r.path("/api/v1/notifications/**", "/api/v1/notification/**")
                        .uri("lb://notification-service"))
                .route("billing-service", r -> r.path(
                                "/api/v1/billing/**",
                                "/api/v1/subscriptions/**",
                                "/api/v1/invoices/**",
                                "/api/v1/payments/**")
                        .uri("lb://billing-service"))
                .route("integration-service", r -> r.path("/api/v1/integrations/**", "/api/v1/integration/**")
                        .uri("lb://integration-service"))
                .route("analytics-service", r -> r.path("/api/v1/analytics/**")
                        .uri("lb://analytics-service"))
                .route("search-service", r -> r.path("/api/v1/search/**")
                        .uri("lb://search-service"))
                .route("media-service", r -> r.path("/api/v1/media/**")
                        .uri("lb://media-service"))
                .route("audit-service", r -> r.path("/api/v1/audit/**")
                        .uri("lb://audit-service"))
                .route("deal-service", r -> r.path(
                                "/api/v1/deals/**",
                                "/api/v1/deal/**",
                                "/api/v1/opportunities/**",
                                "/api/v1/quotes/**")
                        .uri("lb://deal-service"))
                .route("marketplace-service", r -> r.path("/api/v1/marketplace/**")
                        .uri("lb://marketplace-service"))
                .build();
    }
}
