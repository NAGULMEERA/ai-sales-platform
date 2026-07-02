package com.aisales.gateway.support;

import org.springframework.web.server.ServerWebExchange;

/**
 * Resolves the originating client IP for gateway rate limiting. Prefers the first hop in
 * {@code X-Forwarded-For} when present (typical behind a load balancer); otherwise falls back to
 * the direct remote address.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {
    }

    public static String resolve(ServerWebExchange exchange) {
        String forwardedFor = exchange.getRequest().getHeaders().getFirst("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        if (exchange.getRequest().getRemoteAddress() != null
                && exchange.getRequest().getRemoteAddress().getAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        return "unknown";
    }
}
