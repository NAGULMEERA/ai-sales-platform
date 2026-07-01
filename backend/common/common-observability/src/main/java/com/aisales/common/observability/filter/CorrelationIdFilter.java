package com.aisales.common.observability.filter;

import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.core.util.CorrelationIdUtils;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String correlationId = request.getHeader(ApiConstants.CORRELATION_ID_HEADER);
            if (!StringUtils.hasText(correlationId)) {
                correlationId = CorrelationIdUtils.generate();
            }
            CorrelationIdUtils.set(correlationId);
            response.setHeader(ApiConstants.CORRELATION_ID_HEADER, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            CorrelationIdUtils.clear();
        }
    }
}
