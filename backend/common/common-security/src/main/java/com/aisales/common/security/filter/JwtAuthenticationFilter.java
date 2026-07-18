package com.aisales.common.security.filter;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.core.constant.SecurityConstants;
import com.aisales.common.core.persistence.TenantHibernateFilterEnabler;
import com.aisales.common.core.util.MDCUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.common.observability.tracing.TraceContextEnricher;
import com.aisales.common.exception.exception.UnauthorizedException;
import com.aisales.common.security.model.UserPrincipal;
import com.aisales.common.security.util.JwtTokenValidator;
import com.aisales.common.security.util.JwtUtils;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final JwtTokenValidator jwtTokenValidator;
    private final TenantHibernateFilterEnabler tenantHibernateFilterEnabler;
    private final ObjectProvider<TraceContextEnricher> traceContextEnricher;
    private final ObjectProvider<PlatformMetrics> platformMetrics;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    // Access tokens only — refresh tokens must not authenticate business APIs.
                    Claims claims = jwtTokenValidator.validateAccessToken(token);
                    UserPrincipal principal = jwtUtils.toUserPrincipal(claims);
                    if (!validatePropagatedTenantHeader(request, response, principal)) {
                        return;
                    }
                    if (principal.getRoles() != null && principal.getRoles().contains("SUPER_ADMIN")) {
                        TenantContext.setPlatformAdmin(true);
                    }
                    if (principal.getTenantId() != null) {
                        TenantContext.setTenantId(principal.getTenantId());
                    }
                    String organizationId = jwtUtils.extractOrganizationId(claims);
                    if (StringUtils.hasText(organizationId)) {
                        TenantContext.setOrganizationId(organizationId);
                    }
                    var authentication = new UsernamePasswordAuthenticationToken(
                            principal, null, principal.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    TenantContext.setUserId(principal.getUserId());
                    tenantHibernateFilterEnabler.enableTenantFilter();
                } catch (UnauthorizedException ex) {
                    recordJwtFailure(ex.getMessage() != null && ex.getMessage().contains("expired")
                            ? "expired"
                            : "invalid");
                } catch (JwtException | IllegalArgumentException ex) {
                    recordJwtFailure("invalid");
                }
            }
            MDCUtils.putContext();
            traceContextEnricher.ifAvailable(TraceContextEnricher::enrichCurrentSpan);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDCUtils.clearContext();
        }
    }

    private void recordJwtFailure(String reason) {
        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        if (metrics != null) {
            metrics.increment(MetricNames.JWT_VALIDATION_FAILURE, "reason", reason);
        }
    }

    private String resolveToken(HttpServletRequest request) {
        String bearer = request.getHeader(SecurityConstants.AUTHORIZATION_HEADER);
        if (StringUtils.hasText(bearer) && bearer.startsWith(SecurityConstants.BEARER_PREFIX)) {
            return bearer.substring(SecurityConstants.BEARER_PREFIX.length());
        }
        return null;
    }

    private boolean validatePropagatedTenantHeader(HttpServletRequest request, HttpServletResponse response,
                                                   UserPrincipal principal) throws IOException {
        String headerTenant = request.getHeader(ApiConstants.TENANT_ID_HEADER);
        if (!StringUtils.hasText(headerTenant) || !StringUtils.hasText(principal.getTenantId())) {
            return true;
        }
        if (headerTenant.equals(principal.getTenantId())) {
            return true;
        }
        recordJwtFailure("tenant_mismatch");
        response.sendError(HttpStatus.FORBIDDEN.value(), "Tenant header does not match token");
        return false;
    }
}
