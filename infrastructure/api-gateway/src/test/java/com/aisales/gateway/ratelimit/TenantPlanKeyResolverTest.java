package com.aisales.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.common.core.constant.ApiConstants;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class TenantPlanKeyResolverTest {

    private final TenantPlanKeyResolver resolver = new TenantPlanKeyResolver();

    @Test
    void shouldKeyByPremiumTenant() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/ai/execute")
                .header(ApiConstants.TENANT_ID_HEADER, "tenant-1")
                .header(ApiConstants.SUBSCRIPTION_PLAN_HEADER, "PREMIUM")
                .build();

        String key = resolver.resolve(MockServerWebExchange.from(request)).block();

        assertThat(key).isEqualTo("PREMIUM:tenant-1");
    }

    @Test
    void shouldFailClosedWhenTenantHeaderMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/leads").build();

        String key = resolver.resolve(MockServerWebExchange.from(request)).block();

        assertThat(key).isNull();
    }

    @Test
    void shouldNormalizePaidAliasesToPremium() {
        assertThat(TenantPlanKeyResolver.normalizePlan("paid")).isEqualTo("PREMIUM");
        assertThat(TenantPlanKeyResolver.normalizePlan("pro")).isEqualTo("PREMIUM");
        assertThat(TenantPlanKeyResolver.normalizePlan("free")).isEqualTo("FREE");
        assertThat(TenantPlanKeyResolver.normalizePlan(null)).isEqualTo("FREE");
    }
}
