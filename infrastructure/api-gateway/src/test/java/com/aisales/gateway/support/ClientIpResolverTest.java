package com.aisales.gateway.support;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.gateway.config.GatewayRateLimitProperties;
import java.net.InetSocketAddress;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

class ClientIpResolverTest {

    @Test
    void shouldIgnoreSpoofedForwardedForWhenPeerIsNotTrusted() {
        ClientIpResolver resolver = resolverWithTrustedProxies(List.of("10.0.0.0/8"));
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress("203.0.113.50", 8080))
                .header("X-Forwarded-For", "198.51.100.1")
                .build();

        assertThat(resolver.resolve(MockServerWebExchange.from(request))).isEqualTo("203.0.113.50");
    }

    @Test
    void shouldUseClientHopWhenPeerIsTrustedProxy() {
        ClientIpResolver resolver = resolverWithTrustedProxies(List.of("10.0.0.0/8"));
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress("10.0.0.5", 8080))
                .header("X-Forwarded-For", "203.0.113.10, 10.0.0.5")
                .build();

        assertThat(resolver.resolve(MockServerWebExchange.from(request))).isEqualTo("203.0.113.10");
    }

    @Test
    void shouldFallBackToRemoteAddressWhenForwardedForMissing() {
        ClientIpResolver resolver = resolverWithTrustedProxies(List.of());
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
                .remoteAddress(new InetSocketAddress("127.0.0.1", 8080))
                .build();

        assertThat(resolver.resolve(MockServerWebExchange.from(request))).isEqualTo("127.0.0.1");
    }

    private static ClientIpResolver resolverWithTrustedProxies(List<String> cidrs) {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setTrustedProxyCidrs(cidrs);
        return new ClientIpResolver(properties);
    }
}
