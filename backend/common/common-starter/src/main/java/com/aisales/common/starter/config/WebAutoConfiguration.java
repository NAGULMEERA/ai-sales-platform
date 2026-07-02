package com.aisales.common.starter.config;

import com.aisales.common.observability.filter.CorrelationIdFilter;
import com.aisales.common.observability.filter.LoggingFilter;
import com.aisales.common.observability.http.CorrelationIdPropagationInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.boot.web.client.RestTemplateCustomizer;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import({JacksonConfig.class, ValidationConfig.class})
public class WebAutoConfiguration {

    /**
     * Spring Security's {@code FilterChainProxy} registers itself in the servlet container at
     * {@link SecurityProperties#DEFAULT_FILTER_ORDER} (-100 by default). Both observability
     * filters must run strictly before it so that {@code correlation_id} (and the request-scoped
     * timing measured by {@link LoggingFilter}) are already present in MDC while Spring Security's
     * internal filters run (JWT validation, OAuth2 login, authorization) &mdash; otherwise any log
     * line emitted from inside the security chain (e.g. an OAuth2 login failure) would be missing
     * correlation_id/trace_id, which defeats Rule 08's "never lose traceability" requirement.
     */
    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter filter) {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 10);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public FilterRegistrationBean<LoggingFilter> loggingFilterRegistration(LoggingFilter filter) {
        FilterRegistrationBean<LoggingFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.setOrder(SecurityProperties.DEFAULT_FILTER_ORDER - 9);
        registration.addUrlPatterns("/*");
        return registration;
    }

    @Bean
    public RestTemplateCustomizer correlationIdRestTemplateCustomizer() {
        return restTemplate -> restTemplate.getInterceptors().add(new CorrelationIdPropagationInterceptor());
    }
}
