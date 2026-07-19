package com.aisales.common.core.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayPublicPathMatcherTest {

    @Test
    void shouldAllowAuthPaths() {
        assertThat(GatewayPublicPathMatcher.isPublicPath("/api/v1/auth/login")).isTrue();
        assertThat(GatewayPublicPathMatcher.isPublicPath("/api/v1/auth/register")).isTrue();
        assertThat(GatewayPublicPathMatcher.isPublicPath("/api/v1/auth/refresh")).isTrue();
        assertThat(GatewayPublicPathMatcher.isPublicPath("/api/v1/auth/logout")).isFalse();
        assertThat(GatewayPublicPathMatcher.isPublicPath("/api/v1/auth/sessions")).isFalse();
    }

    @Test
    void shouldRequireAuthForTenantManagement() {
        assertThat(GatewayPublicPathMatcher.isPublicPath("/api/v1/tenants")).isFalse();
        assertThat(GatewayPublicPathMatcher.isPublicPath("/api/v1/tenants/11111111-1111-1111-1111-111111111111")).isFalse();
    }

    @Test
    void shouldAllowStripePaymentWebhooks() {
        assertThat(GatewayPublicPathMatcher.isPublicPath("/api/v1/payments/webhooks/stripe")).isTrue();
        assertThat(GatewayPublicPathMatcher.isPublicPath("/api/v1/payments/abc")).isFalse();
    }

    @Test
    void shouldAllowMetaLeadAdsWebhooks() {
        assertThat(GatewayPublicPathMatcher.isPublicPath("/api/v1/integrations/webhooks/meta/leadgen"))
                .isTrue();
        assertThat(GatewayPublicPathMatcher.isPublicPath("/api/v1/integrations/config")).isFalse();
    }
}
