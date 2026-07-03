package com.aisales.common.observability.filter;

import com.aisales.common.observability.config.ObservabilityProperties;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
@RequiredArgsConstructor
public class LoggingFilter extends OncePerRequestFilter {

    private final ObservabilityProperties observabilityProperties;
    private final PlatformMetrics platformMetrics;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.currentTimeMillis();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long duration = System.currentTimeMillis() - start;
            logRequest(request, response, duration);
        }
    }

    private void logRequest(HttpServletRequest request, HttpServletResponse response, long durationMs) {
        String method = request.getMethod();
        String uri = request.getRequestURI();
        String status = String.valueOf(response.getStatus());
        if (durationMs >= observabilityProperties.getSlowRequestThreshold().toMillis()) {
            log.warn("slow_http_request method={} uri={} status={} duration_ms={}", method, uri, status, durationMs);
            platformMetrics.increment(MetricNames.HTTP_SLOW_REQUEST,
                    "method", method, "uri", uri, "status", status);
        } else {
            log.info("{} {} {} {}ms", method, uri, status, durationMs);
        }
        if (response.getStatus() >= 500) {
            platformMetrics.increment(MetricNames.HTTP_SERVER_ERROR,
                    "method", method, "uri", uri, "status", status);
        }
    }
}
