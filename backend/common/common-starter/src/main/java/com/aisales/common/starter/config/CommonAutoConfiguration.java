package com.aisales.common.starter.config;

import com.aisales.common.core.config.CoreConfig;
import com.aisales.common.exception.config.GlobalExceptionHandler;
import com.aisales.common.observability.filter.CorrelationIdFilter;
import com.aisales.common.observability.filter.LoggingFilter;
import com.aisales.common.observability.metrics.CustomMetrics;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.common.observability.metrics.AiRequestMetrics;
import com.aisales.common.observability.metrics.MetricsConfig;
import com.aisales.common.observability.metrics.TimerAspect;
import com.aisales.common.observability.resilience.CircuitBreakerObservabilityConfig;
import com.aisales.common.observability.resilience.RetryObservabilityConfig;
import com.aisales.common.observability.tracing.TraceContextEnricher;
import com.aisales.common.observability.tracing.TracingConfig;
import com.aisales.common.security.filter.JwtAuthenticationFilter;
import com.aisales.common.security.util.JwtTokenProvider;
import com.aisales.common.security.util.JwtTokenValidator;
import com.aisales.common.security.util.JwtUtils;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({CoreConfig.class, GlobalExceptionHandler.class, MetricsConfig.class, TracingConfig.class,
        RetryObservabilityConfig.class, CircuitBreakerObservabilityConfig.class})
@ComponentScan(basePackageClasses = {
        CorrelationIdFilter.class,
        LoggingFilter.class,
        CustomMetrics.class,
        PlatformMetrics.class,
        AiRequestMetrics.class,
        TimerAspect.class,
        TraceContextEnricher.class,
        JwtAuthenticationFilter.class,
        JwtUtils.class,
        JwtTokenProvider.class,
        JwtTokenValidator.class,
        com.aisales.common.security.aspect.TenantAuthorizationAspect.class,
        com.aisales.common.core.persistence.TenantHibernateFilterEnabler.class,
        com.aisales.common.core.persistence.TenantRlsConnectionInitializer.class,
        com.aisales.common.core.audit.AuditableAspect.class,
        com.aisales.common.core.web.ApiResponseEnrichmentAdvice.class
})
public class CommonAutoConfiguration {
}
