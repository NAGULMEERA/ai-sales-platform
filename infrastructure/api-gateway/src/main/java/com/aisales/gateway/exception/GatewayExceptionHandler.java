package com.aisales.gateway.exception;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.common.exception.model.ErrorResponse;
import com.aisales.gateway.config.GatewayRateLimitProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(-2)
@RequiredArgsConstructor
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private final GatewayRateLimitProperties rateLimitProperties;
    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        String path = exchange.getRequest().getURI().getPath();
        ErrorCode errorCode = resolveErrorCode(status, path);

        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            int retryAfter = Math.max(1, rateLimitProperties.getRetryAfterSeconds());
            exchange.getResponse().getHeaders().set(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfter));
            log.warn("Gateway rate limit exceeded path={} code={} client={}",
                    path,
                    errorCode.getCode(),
                    exchange.getRequest().getHeaders().getFirst("X-Forwarded-For"));
        }

        String correlationId = exchange.getRequest().getHeaders().getFirst(ApiConstants.CORRELATION_ID_HEADER);
        ErrorResponse body = ErrorResponse.of(
                errorCode,
                errorCode.getDefaultMessage(),
                path,
                correlationId);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException jsonEx) {
            bytes = ("{\"status\":" + status.value() + ",\"message\":\"Request failed\"}").getBytes();
        }
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    static ErrorCode resolveErrorCode(HttpStatus status, String path) {
        if (status != HttpStatus.TOO_MANY_REQUESTS) {
            return ErrorCode.INTERNAL_ERROR;
        }
        if (!StringUtils.hasText(path)) {
            return ErrorCode.RATE_LIMIT_EXCEEDED;
        }
        if (path.startsWith("/api/v1/auth/")) {
            return ErrorCode.AUTH_RATE_LIMIT;
        }
        if (path.startsWith("/api/v1/ai/")
                || path.startsWith("/api/v1/ai-quota")
                || path.startsWith("/api/v1/prompts/")
                || path.startsWith("/api/v1/embeddings/")
                || path.startsWith("/api/v1/token-usage/")
                || path.startsWith("/api/v1/knowledge-")) {
            // Edge RPS (AI_001). Soft daily budgets use AI_003 from ai-service, not this path.
            return ErrorCode.AI_RATE_LIMIT;
        }
        return ErrorCode.RATE_LIMIT_EXCEEDED;
    }

    private static HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException
                && responseStatusException.getStatusCode() instanceof HttpStatus httpStatus) {
            return httpStatus;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
