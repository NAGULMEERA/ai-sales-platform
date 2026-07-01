package com.aisales.gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator customRouteLocator(RouteLocatorBuilder builder) {
        return builder.routes()
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
                .route("ai-service", r -> r.path("/api/v1/ai/**", "/api/v1/prompts/**", "/api/v1/embeddings/**")
                        .uri("lb://ai-service"))
                .route("workflow-service", r -> r.path("/api/v1/workflows/**", "/api/v1/workflow/**")
                        .uri("lb://workflow-service"))
                .route("notification-service", r -> r.path("/api/v1/notifications/**", "/api/v1/notification/**")
                        .uri("lb://notification-service"))
                .route("billing-service", r -> r.path("/api/v1/billing/**", "/api/v1/subscriptions/**")
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
                .route("deal-service", r -> r.path("/api/v1/deals/**", "/api/v1/deal/**")
                        .uri("lb://deal-service"))
                .route("marketplace-service", r -> r.path("/api/v1/marketplace/**")
                        .uri("lb://marketplace-service"))
                .build();
    }
}
