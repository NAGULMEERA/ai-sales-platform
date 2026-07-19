package com.aisales.integration.infrastructure.configuration;

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
    public RequestInterceptor integrationFeignInterceptor(IntegrationServiceAuthProperties serviceAuth) {
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

            if (!template.headers().containsKey(SecurityConstants.AUTHORIZATION_HEADER)
                    && StringUtils.hasText(serviceAuth.getBearerToken())) {
                template.header(
                        SecurityConstants.AUTHORIZATION_HEADER,
                        SecurityConstants.BEARER_PREFIX + serviceAuth.getBearerToken());
            }

            if (!template.headers().containsKey(ApiConstants.TENANT_ID_HEADER)
                    && StringUtils.hasText(TenantContext.getTenantId())) {
                template.header(ApiConstants.TENANT_ID_HEADER, TenantContext.getTenantId());
            }
            if (!template.headers().containsKey(ApiConstants.ORGANIZATION_ID_HEADER)
                    && StringUtils.hasText(TenantContext.getOrganizationId())) {
                template.header(ApiConstants.ORGANIZATION_ID_HEADER, TenantContext.getOrganizationId());
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
