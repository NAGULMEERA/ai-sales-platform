package com.aisales.gateway.ratelimit;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.gateway.support.ClientIpResolver;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Rate-limit key for authenticated SaaS routes: {@code PLAN:tenantId} (falls back to IP).
 * Plan comes from {@link ApiConstants#SUBSCRIPTION_PLAN_HEADER} set by the JWT filter.
 */
@Component
@RequiredArgsConstructor
public class TenantPlanKeyResolver implements KeyResolver {

    private final ClientIpResolver clientIpResolver;

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String plan = normalizePlan(
                exchange.getRequest().getHeaders().getFirst(ApiConstants.SUBSCRIPTION_PLAN_HEADER));
        String tenantId = exchange.getRequest().getHeaders().getFirst(ApiConstants.TENANT_ID_HEADER);
        String identity = StringUtils.hasText(tenantId) ? tenantId.trim() : clientIpResolver.resolve(exchange);
        return Mono.just(plan + ":" + identity);
    }

    static String normalizePlan(String raw) {
        if (!StringUtils.hasText(raw)) {
            return "FREE";
        }
        String plan = raw.trim().toUpperCase(Locale.ROOT);
        if ("PREMIUM".equals(plan) || "PAID".equals(plan) || "PRO".equals(plan)) {
            return "PREMIUM";
        }
        return "FREE";
    }
}
