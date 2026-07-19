package com.aisales.gateway.security;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.core.security.GatewayPublicPathMatcher;
import com.aisales.common.core.security.JwtClaimExtractor;
import com.aisales.gateway.config.GatewayJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Validates platform JWTs and propagates tenant/user context headers to downstream services.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements GlobalFilter, Ordered {

    private final GatewayJwtProperties jwtProperties;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (GatewayPublicPathMatcher.isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        String token = authHeader.substring("Bearer ".length());
        try {
            Claims claims = new JwtClaimExtractor(
                            jwtProperties.getPublicKeyLocation(),
                            jwtProperties.getIssuer(),
                            jwtProperties.getAudience(),
                            jwtProperties.isRequireIssuerAudience())
                    .parseAndValidateAccessToken(token);
            ServerWebExchange mutated = exchange.mutate()
                    .request(builder -> enrichDownstreamHeaders(builder, claims))
                    .build();
            return chain.filter(mutated);
        } catch (JwtException ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private void enrichDownstreamHeaders(ServerHttpRequest.Builder builder, Claims claims) {
        String tenantId = JwtClaimExtractor.extractTenantId(claims);
        if (StringUtils.hasText(tenantId)) {
            builder.header(ApiConstants.TENANT_ID_HEADER, tenantId);
        }
        String userId = JwtClaimExtractor.extractUserId(claims);
        if (StringUtils.hasText(userId)) {
            builder.header(ApiConstants.USER_ID_HEADER, userId);
        }
        String organizationId = JwtClaimExtractor.extractOrganizationId(claims);
        if (StringUtils.hasText(organizationId)) {
            builder.header(ApiConstants.ORGANIZATION_ID_HEADER, organizationId);
        }
        String subscriptionPlan = JwtClaimExtractor.extractSubscriptionPlan(claims);
        if (StringUtils.hasText(subscriptionPlan)) {
            builder.header(ApiConstants.SUBSCRIPTION_PLAN_HEADER, subscriptionPlan);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 3;
    }
}
