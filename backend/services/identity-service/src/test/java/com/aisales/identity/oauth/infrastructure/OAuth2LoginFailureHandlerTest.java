package com.aisales.identity.oauth.infrastructure;

import com.aisales.identity.audit.application.AuditService;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.exception.model.ErrorCode;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;



import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OAuth2LoginFailureHandlerTest {

    private final JsonMapper objectMapper = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();
    private final AuditService auditService = mock(AuditService.class);
    private final OAuth2LoginFailureHandler handler = new OAuth2LoginFailureHandler(objectMapper, auditService);

    @AfterEach
    void tearDown() {
        CorrelationIdUtils.clear();
    }

    @Test
    void shouldReturnStandardErrorResponseOnOAuth2Failure() throws Exception {
        CorrelationIdUtils.set("test-correlation-id");
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/google");
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationException exception =
                new OAuth2AuthenticationException(new OAuth2Error("access_denied"), "Access denied by user");

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getStatus()).isEqualTo(ErrorCode.AUTH_OAUTH2_LOGIN_FAILED.getHttpStatus().value());
        assertThat(response.getContentType()).isEqualTo("application/json");

        JsonNode body = objectMapper.readTree(response.getContentAsByteArray());
        assertThat(body.get("correlationId").asText()).isEqualTo("test-correlation-id");
        assertThat(body.get("path").asText()).isEqualTo("/login/oauth2/code/google");
        assertThat(body.get("error").get("code").asText())
                .isEqualTo(ErrorCode.AUTH_OAUTH2_LOGIN_FAILED.getCode());
    }

    @Test
    void shouldNeverRedirectOrLeakStackTraceOnFailure() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRequestURI()).thenReturn("/login/oauth2/code/google");
        MockHttpServletResponse response = new MockHttpServletResponse();
        OAuth2AuthenticationException exception = new OAuth2AuthenticationException(
                new OAuth2Error("server_error"), "JWK Set fetch failed", new SocketTimeoutException("Read timed out"));

        handler.onAuthenticationFailure(request, response, exception);

        assertThat(response.getRedirectedUrl()).isNull();
        String body = response.getContentAsString();
        assertThat(body).doesNotContain("SocketTimeoutException");
        assertThat(body).doesNotContain("at com.aisales");
    }
}
