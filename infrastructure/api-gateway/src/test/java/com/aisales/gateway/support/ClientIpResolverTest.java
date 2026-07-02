package com.aisales.gateway.support;

import org.junit.jupiter.api.Test;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;

import static org.assertj.core.api.Assertions.assertThat;

class ClientIpResolverTest {

    @Test
    void shouldUseFirstForwardedForHopWhenPresent() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
                .header("X-Forwarded-For", "203.0.113.10, 198.51.100.2")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        assertThat(ClientIpResolver.resolve(exchange)).isEqualTo("203.0.113.10");
    }

    @Test
    void shouldFallBackToRemoteAddressWhenForwardedForMissing() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/v1/auth/login")
                .remoteAddress(new java.net.InetSocketAddress("127.0.0.1", 8080))
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        assertThat(ClientIpResolver.resolve(exchange)).isEqualTo("127.0.0.1");
    }
}
