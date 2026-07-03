package com.aisales.common.starter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

/**
 * Platform-owned OpenAPI defaults for every service using {@code common-starter}.
 *
 * <p>Ownership model:
 * <ul>
 *   <li>Platform: single {@link OpenAPI} bean, JWT security scheme, springdoc paths (see
 *       {@code platform/application-openapi.yml})</li>
 *   <li>Service: optional {@code aisales.api.*} properties for title/description/version only</li>
 *   <li>Advanced: define an {@code org.springdoc.core.customizers.OpenApiCustomizer} bean; do not
 *       register a second {@link OpenAPI} bean unless replacing platform defaults entirely</li>
 * </ul>
 */
@AutoConfiguration
@ConditionalOnWebApplication
@ConditionalOnClass(OpenAPI.class)
@EnableConfigurationProperties(AisalesApiProperties.class)
public class OpenApiAutoConfiguration {

    public static final String BEARER_SCHEME = "Bearer Authentication";

    @Bean
    @ConditionalOnMissingBean(OpenAPI.class)
    public OpenAPI platformOpenApi(
            AisalesApiProperties apiProperties,
            @Value("${spring.application.name:AI Sales Platform}") String applicationName) {
        String title = StringUtils.hasText(apiProperties.getTitle()) ? apiProperties.getTitle() : applicationName;
        return new OpenAPI()
                .info(new Info()
                        .title(title)
                        .version(apiProperties.getVersion())
                        .description(apiProperties.getDescription())
                        .contact(new Contact()
                                .name(apiProperties.getContactName())
                                .email(apiProperties.getContactEmail())))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
