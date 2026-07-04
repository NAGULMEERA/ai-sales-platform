package com.aisales.identity.oauth.infrastructure;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.common.exception.model.ErrorResponse;
import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.audit.domain.AuditDetails;
import com.aisales.common.observability.http.OutboundCallDiagnostics;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.MDC;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;



/**
 * Replaces Spring Security's default {@code SimpleUrlAuthenticationFailureHandler} (a
 * browser-oriented redirect to {@code /login?error} with no structured logging) so that every
 * Google OAuth2 login failure — authorization-code exchange failure, UserInfo call failure, ID
 * token/JWK validation failure, provider timeout, etc. — is (1) logged with the same
 * target/outcome fields used for every other outbound call in this platform (correlation_id and
 * trace_id are already in MDC by this point, since {@code CorrelationIdFilter} now runs before
 * Spring Security's filter chain) and (2) returned to the client as the platform's standard
 * {@link ErrorResponse} JSON contract, matching {@link OAuth2AuthenticationSuccessHandler}'s
 * JSON-based response style instead of a server-side redirect.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginFailureHandler implements AuthenticationFailureHandler {

    private static final String TARGET_SERVICE = "google-oauth2";

    private final ObjectMapper objectMapper;
    private final AuditService auditService;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                         AuthenticationException exception) throws IOException {
        log.warn("OAuth2 login failed {} {} {}",
                StructuredArguments.kv("target_service", TARGET_SERVICE),
                StructuredArguments.kv("outcome", OutboundCallDiagnostics.outcome(exception)),
                StructuredArguments.kv("error", exception.getMessage()));

        auditService.logSecurityEvent(null, null, AuditAction.OAUTH_LOGIN_FAILED, "oauth",
                TARGET_SERVICE, request.getRemoteAddr(), request.getHeader("User-Agent"),
                AuditDetails.reason(OutboundCallDiagnostics.outcome(exception)));

        ErrorResponse errorResponse = ErrorResponse.of(
                ErrorCode.AUTH_OAUTH2_LOGIN_FAILED,
                null,
                request.getRequestURI(),
                CorrelationIdUtils.getCorrelationId(),
                currentTraceId(),
                TenantContext.getTenantId(),
                TenantContext.getUserId());

        response.setStatus(ErrorCode.AUTH_OAUTH2_LOGIN_FAILED.getHttpStatus().value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), errorResponse);
    }

    private String currentTraceId() {
        String traceId = MDC.get("traceId");
        return traceId != null ? traceId : MDC.get("trace_id");
    }
}
