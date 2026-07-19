package com.aisales.gateway.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.gateway.support.ClientIpResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

@ExtendWith(MockitoExtension.class)
class TenantPlanKeyResolverTest {

    @Mock private ClientIpResolver clientIpResolver;

    @Test
    void shouldKeyByPremiumTenant() {
        TenantPlanKeyResolver resolver = new TenantPlanKeyResolver(clientIpResolver);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/ai/execute")
                .header(ApiConstants.TENANT_ID_HEADER, "tenant-1")
                .header(ApiConstants.SUBSCRIPTION_PLAN_HEADER, "PREMIUM")
                .build();

        String key = resolver.resolve(MockServerWebExchange.from(request)).block();

        assertThat(key).isEqualTo("PREMIUM:tenant-1");
    }

    @Test
    void shouldDefaultToFreeAndFallbackToIp() {
        when(clientIpResolver.resolve(org.mockito.ArgumentMatchers.any())).thenReturn("203.0.113.9");
        TenantPlanKeyResolver resolver = new TenantPlanKeyResolver(clientIpResolver);
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/leads").build();

        String key = resolver.resolve(MockServerWebExchange.from(request)).block();

        assertThat(key).isEqualTo("FREE:203.0.113.9");
    }

    @Test
    void shouldNormalizePaidAliasesToPremium() {
        assertThat(TenantPlanKeyResolver.normalizePlan("paid")).isEqualTo("PREMIUM");
        assertThat(TenantPlanKeyResolver.normalizePlan("pro")).isEqualTo("PREMIUM");
        assertThat(TenantPlanKeyResolver.normalizePlan("free")).isEqualTo("FREE");
        assertThat(TenantPlanKeyResolver.normalizePlan(null)).isEqualTo("FREE");
    }
}
