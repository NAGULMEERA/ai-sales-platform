package com.aisales.gateway.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.common.exception.model.ErrorCode;
import com.aisales.gateway.config.GatewayRateLimitProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ResponseStatusException;

class GatewayExceptionHandlerTest {

    private GatewayExceptionHandler handler;

    @BeforeEach
    void setUp() {
        GatewayRateLimitProperties properties = new GatewayRateLimitProperties();
        properties.setRetryAfterSeconds(2);
        handler = new GatewayExceptionHandler(properties);
    }

    @Test
    void shouldReturn429WithJsonContentTypeForRateLimitExceeded() {
        MockServerHttpRequest request = MockServerHttpRequest.post("/api/v1/auth/login").build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        handler.handle(exchange, new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS)).block();

        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(exchange.getResponse().getHeaders().getFirst(HttpHeaders.RETRY_AFTER)).isEqualTo("2");
    }

    @Test
    void shouldUseAiRateLimitCodeForAiPaths() {
        assertThat(GatewayExceptionHandler.resolveErrorCode(HttpStatus.TOO_MANY_REQUESTS, "/api/v1/ai/execute"))
                .isEqualTo(ErrorCode.AI_RATE_LIMIT);
    }

    @Test
    void shouldUseAuthRateLimitCodeForAuthPaths() {
        assertThat(GatewayExceptionHandler.resolveErrorCode(HttpStatus.TOO_MANY_REQUESTS, "/api/v1/auth/login"))
                .isEqualTo(ErrorCode.AUTH_RATE_LIMIT);
    }

    @Test
    void shouldUseGenericRateLimitCodeForOtherPaths() {
        assertThat(GatewayExceptionHandler.resolveErrorCode(HttpStatus.TOO_MANY_REQUESTS, "/api/v1/leads"))
                .isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED);
    }
}
