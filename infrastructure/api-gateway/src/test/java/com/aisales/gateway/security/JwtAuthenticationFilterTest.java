package com.aisales.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.security.jwt.JwtRsaProperties;
import com.aisales.common.security.jwt.PlatformRsaKeyProvider;
import com.aisales.common.security.util.JwtTokenProvider;
import com.aisales.gateway.config.GatewayJwtProperties;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private JwtTokenProvider tokenProvider;
    private GatewayFilterChain chain;

    @BeforeEach
    void setUp() {
        GatewayJwtProperties gatewayJwt = new GatewayJwtProperties();
        gatewayJwt.setPublicKeyLocation("classpath:jwt/local-public.pem");
        gatewayJwt.setIssuer("aisales-platform");
        gatewayJwt.setAudience("aisales-api");
        gatewayJwt.setRequireIssuerAudience(true);
        filter = new JwtAuthenticationFilter(gatewayJwt);

        JwtRsaProperties properties = new JwtRsaProperties();
        properties.setSigningEnabled(true);
        properties.setPrivateKeyLocation("classpath:jwt/local-private.pem");
        properties.setPublicKeyLocation("classpath:jwt/local-public.pem");
        properties.setKeyId("aisales-1");
        properties.setIssuer("aisales-platform");
        properties.setAudience("aisales-api");
        properties.setAccessTokenExpirationMs(3_600_000L);
        properties.setRefreshTokenExpirationMs(86_400_000L);

        MockEnvironment env = new MockEnvironment();
        env.setActiveProfiles("test");
        PlatformRsaKeyProvider keyProvider = new PlatformRsaKeyProvider(properties, env);
        ReflectionTestUtils.invokeMethod(keyProvider, "init");
        tokenProvider = new JwtTokenProvider(keyProvider, properties);
        chain = mock(GatewayFilterChain.class);
        when(chain.filter(any(ServerWebExchange.class))).thenReturn(Mono.empty());
    }

    @Test
    void shouldBypassPublicAuthPaths() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.post("/api/v1/auth/login").build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(exchange);
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void shouldRejectMissingBearerOnProtectedPath() {
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/v1/leads").build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void shouldRejectInvalidToken() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/leads")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                .build());

        filter.filter(exchange, chain).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(chain, never()).filter(any());
    }

    @Test
    void shouldPropagateTenantAndUserHeadersForValidAccessToken() {
        var tokens = tokenProvider.generateTokens(
                "user-1", "tenant-1", "org-1", "a@b.com", Set.of("AGENT"), Set.of("lead:read"));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/leads")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.getAccessToken())
                .build());

        filter.filter(exchange, chain).block();

        verify(chain).filter(any(ServerWebExchange.class));
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void shouldEnrichMutatedRequestWithTenantHeader() {
        var tokens = tokenProvider.generateTokens(
                "user-9", "tenant-9", "org-9", "u@x.com", Set.of("ADMIN"), Set.of());
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/customers")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.getAccessToken())
                .build());

        filter.filter(exchange, chain).block();

        org.mockito.ArgumentCaptor<ServerWebExchange> captor =
                org.mockito.ArgumentCaptor.forClass(ServerWebExchange.class);
        verify(chain).filter(captor.capture());
        ServerWebExchange mutated = captor.getValue();
        assertThat(mutated.getRequest().getHeaders().getFirst(ApiConstants.TENANT_ID_HEADER))
                .isEqualTo("tenant-9");
        assertThat(mutated.getRequest().getHeaders().getFirst(ApiConstants.USER_ID_HEADER))
                .isEqualTo("user-9");
        assertThat(mutated.getRequest().getHeaders().getFirst(ApiConstants.ORGANIZATION_ID_HEADER))
                .isEqualTo("org-9");
    }
}
