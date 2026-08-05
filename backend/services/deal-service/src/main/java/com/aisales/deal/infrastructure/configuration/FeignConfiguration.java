package com.aisales.deal.infrastructure.configuration;

import com.aisales.common.contracts.config.FeignClientsConfig;
import com.aisales.common.core.constant.ApiConstants;
import com.aisales.common.core.constant.SecurityConstants;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.core.util.TenantContext;
import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
@Import(FeignClientsConfig.class)
public class FeignConfiguration {

    @Bean
    public RequestInterceptor feignAuthPropagationInterceptor() {
        return template -> {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                copyHeader(template, request, SecurityConstants.AUTHORIZATION_HEADER);
                copyHeader(template, request, ApiConstants.CORRELATION_ID_HEADER);
                copyHeader(template, request, ApiConstants.TENANT_ID_HEADER);
                copyHeader(template, request, ApiConstants.ORGANIZATION_ID_HEADER);
            }
            // Background threads (Kafka consumers, schedulers, async tasks) have no servlet
            // request but still carry tenant context via the ThreadLocal; fall back to it so
            // downstream calls originating off the request thread remain tenant-scoped.
            if (!template.headers().containsKey(ApiConstants.TENANT_ID_HEADER)) {
                String tenantId = TenantContext.getTenantId();
                if (StringUtils.hasText(tenantId)) {
                    template.header(ApiConstants.TENANT_ID_HEADER, tenantId);
                }
            }
            if (!template.headers().containsKey(ApiConstants.ORGANIZATION_ID_HEADER)) {
                String organizationId = TenantContext.getOrganizationId();
                if (StringUtils.hasText(organizationId)) {
                    template.header(ApiConstants.ORGANIZATION_ID_HEADER, organizationId);
                }
            }
            CorrelationIdUtils.get().ifPresent(correlationId -> {
                if (!template.headers().containsKey(ApiConstants.CORRELATION_ID_HEADER)) {
                    template.header(ApiConstants.CORRELATION_ID_HEADER, correlationId);
                }
            });
        };
    }

    private static void copyHeader(feign.RequestTemplate template, HttpServletRequest request, String name) {
        String value = request.getHeader(name);
        if (StringUtils.hasText(value)) {
            template.header(name, value);
        }
    }
}
