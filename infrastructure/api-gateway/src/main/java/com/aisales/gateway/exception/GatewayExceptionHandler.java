package com.aisales.gateway.exception;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.common.exception.model.ErrorResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.webflux.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
@Order(-2)
public class GatewayExceptionHandler implements ErrorWebExceptionHandler {

    private final JsonMapper objectMapper = JsonMapper.builder().build();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        if (exchange.getResponse().isCommitted()) {
            return Mono.error(ex);
        }

        HttpStatus status = resolveStatus(ex);
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);

        if (status == HttpStatus.TOO_MANY_REQUESTS) {
            log.warn("Gateway rate limit exceeded path={} client={}",
                    exchange.getRequest().getURI().getPath(),
                    exchange.getRequest().getHeaders().getFirst("X-Forwarded-For"));
        }

        String correlationId = exchange.getRequest().getHeaders().getFirst(ApiConstants.CORRELATION_ID_HEADER);
        ErrorResponse body = ErrorResponse.of(
                status == HttpStatus.TOO_MANY_REQUESTS ? ErrorCode.AUTH_RATE_LIMIT : ErrorCode.INTERNAL_ERROR,
                status == HttpStatus.TOO_MANY_REQUESTS ? ErrorCode.AUTH_RATE_LIMIT.getDefaultMessage() : ex.getMessage(),
                exchange.getRequest().getURI().getPath(),
                correlationId);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(body);
        } catch (JsonProcessingException jsonEx) {
            bytes = ("{\"status\":" + status.value() + ",\"message\":\"Request failed\"}").getBytes();
        }
        return exchange.getResponse().writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    private static HttpStatus resolveStatus(Throwable ex) {
        if (ex instanceof ResponseStatusException responseStatusException
                && responseStatusException.getStatusCode() instanceof HttpStatus httpStatus) {
            return httpStatus;
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }
}
