package com.aisales.common.observability.tracing;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class TraceContextEnricher {

    private final Tracer tracer;

    @Value("${spring.application.name:unknown}")
    private String serviceName;

    public TraceContextEnricher(@Autowired(required = false) Tracer tracer) {
        this.tracer = tracer;
    }

    public void enrichCurrentSpan() {
        if (tracer == null) {
            return;
        }
        Span span = tracer.currentSpan();
        if (span == null) {
            return;
        }
        tagIfPresent(span, "tenant.id", TenantContext.getTenantId());
        tagIfPresent(span, "organization.id", TenantContext.getOrganizationId());
        tagIfPresent(span, "user.id", TenantContext.getUserId());
        tagIfPresent(span, "request.id", CorrelationIdUtils.getCorrelationId());
        tagIfPresent(span, "service.name", serviceName);
    }

    public void enrichSpan(Span span) {
        if (span == null) {
            return;
        }
        tagIfPresent(span, "tenant.id", TenantContext.getTenantId());
        tagIfPresent(span, "organization.id", TenantContext.getOrganizationId());
        tagIfPresent(span, "user.id", TenantContext.getUserId());
        tagIfPresent(span, "request.id", CorrelationIdUtils.getCorrelationId());
        tagIfPresent(span, "service.name", serviceName);
    }

    private static void tagIfPresent(Span span, String name, String value) {
        if (StringUtils.hasText(value)) {
            span.tag(name, value);
        }
    }
}
