package com.aisales.gateway.ratelimit;

import com.aisales.common.core.constant.ApiConstants;
import java.util.Locale;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Rate-limit key for authenticated SaaS routes: {@code PLAN:tenantId}.
 * Missing tenant fails closed ({@link Mono#empty()}); pair with {@code denyEmptyKey=true}
 * so requests without a tenant key are rejected rather than falling back to a shared IP bucket.
 */
@Component
public class TenantPlanKeyResolver implements KeyResolver {

    @Override
    public Mono<String> resolve(ServerWebExchange exchange) {
        String tenantId = exchange.getRequest().getHeaders().getFirst(ApiConstants.TENANT_ID_HEADER);
        if (!StringUtils.hasText(tenantId)) {
            return Mono.empty();
        }
        String plan = normalizePlan(
                exchange.getRequest().getHeaders().getFirst(ApiConstants.SUBSCRIPTION_PLAN_HEADER));
        return Mono.just(plan + ":" + tenantId.trim());
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
