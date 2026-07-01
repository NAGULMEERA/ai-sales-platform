package com.aisales.gateway.filter;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.core.util.CorrelationIdUtils;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(ApiConstants.CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = CorrelationIdUtils.generate();
        }
        String finalCorrelationId = correlationId;
        ServerWebExchange mutated = exchange.mutate()
                .request(builder -> builder.header(ApiConstants.CORRELATION_ID_HEADER, finalCorrelationId))
                .build();
        return chain.filter(mutated);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
