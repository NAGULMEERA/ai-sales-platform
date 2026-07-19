package com.aisales.common.security.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.core.constant.SecurityConstants;
import com.aisales.common.core.persistence.TenantHibernateFilterEnabler;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.common.observability.tracing.TraceContextEnricher;
import com.aisales.common.security.jwt.JwtRsaProperties;
import com.aisales.common.security.jwt.PlatformRsaKeyProvider;
import com.aisales.common.security.model.UserPrincipal;
import com.aisales.common.security.util.JwtTokenProvider;
import com.aisales.common.security.util.JwtTokenValidator;
import com.aisales.common.security.util.JwtUtils;
import jakarta.servlet.FilterChain;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class JwtAuthenticationFilterTest {

    private JwtAuthenticationFilter filter;
    private JwtTokenProvider tokenProvider;
    private TenantHibernateFilterEnabler tenantHibernateFilterEnabler;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
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
        JwtUtils jwtUtils = new JwtUtils(keyProvider);
        JwtTokenValidator validator = new JwtTokenValidator(jwtUtils, properties);
        tenantHibernateFilterEnabler = mock(TenantHibernateFilterEnabler.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<TraceContextEnricher> traceProvider = mock(ObjectProvider.class);
        @SuppressWarnings("unchecked")
        ObjectProvider<PlatformMetrics> metricsProvider = mock(ObjectProvider.class);

        filter = new JwtAuthenticationFilter(
                jwtUtils, validator, tenantHibernateFilterEnabler, traceProvider, metricsProvider);
        filterChain = mock(FilterChain.class);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    @Test
    void shouldAuthenticateAccessTokenAndSetTenantContext() throws Exception {
        var tokens = tokenProvider.generateTokens(
                "user-1", "tenant-1", "org-1", "a@b.com", Set.of("AGENT"), Set.of("lead:read"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.AUTHORIZATION_HEADER, SecurityConstants.BEARER_PREFIX + tokens.getAccessToken());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(tenantHibernateFilterEnabler).enableTenantFilter();
        // Filter clears TenantContext in finally; authentication must have been set during chain.
        assertThat(response.getStatus()).isEqualTo(HttpStatus.OK.value());
    }

    @Test
    void shouldRejectMismatchedTenantHeader() throws Exception {
        var tokens = tokenProvider.generateTokens(
                "user-1", "tenant-1", "org-1", "a@b.com", Set.of("AGENT"), Set.of("lead:read"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.AUTHORIZATION_HEADER, SecurityConstants.BEARER_PREFIX + tokens.getAccessToken());
        request.addHeader(ApiConstants.TENANT_ID_HEADER, "other-tenant");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(HttpStatus.FORBIDDEN.value());
        verifyNoInteractions(filterChain);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldContinueWithoutAuthWhenTokenMissing() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(tenantHibernateFilterEnabler);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
    }

    @Test
    void shouldNotAuthenticateInvalidTokenButStillContinueChain() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.AUTHORIZATION_HEADER, SecurityConstants.BEARER_PREFIX + "not-a-jwt");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
        verifyNoInteractions(tenantHibernateFilterEnabler);
    }

    @Test
    void shouldPopulateAuthoritiesFromPermissions() throws Exception {
        var tokens = tokenProvider.generateTokens(
                "user-1", "tenant-1", "org-1", "a@b.com", Set.of("USER"), Set.of("ai:execute", "lead:read"));

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(SecurityConstants.AUTHORIZATION_HEADER, SecurityConstants.BEARER_PREFIX + tokens.getAccessToken());
        MockHttpServletResponse response = new MockHttpServletResponse();

        FilterChain capturingChain = (req, res) -> {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            assertThat(auth).isNotNull();
            assertThat(auth.getPrincipal()).isInstanceOf(UserPrincipal.class);
            assertThat(auth.getAuthorities())
                    .extracting(a -> a.getAuthority())
                    .contains("ROLE_USER", "ai:execute", "lead:read");
            assertThat(TenantContext.getTenantId()).isEqualTo("tenant-1");
            assertThat(TenantContext.getOrganizationId()).isEqualTo("org-1");
        };

        filter.doFilter(request, response, capturingChain);
    }
}
