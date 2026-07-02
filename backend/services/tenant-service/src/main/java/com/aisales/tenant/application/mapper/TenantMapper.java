package com.aisales.tenant.application.mapper;

import com.aisales.common.contracts.tenant.TenantDto;
import com.aisales.tenant.domain.entity.Tenant;
import org.springframework.stereotype.Component;

@Component
public class TenantMapper {

    public TenantDto toDto(Tenant tenant) {
        return TenantDto.builder()
                .id(tenant.getId())
                .tenantCode(tenant.getTenantCode())
                .slug(tenant.getSlug())
                .name(tenant.getName())
                .industry(tenant.getIndustry())
                .subscriptionPlan(tenant.getSubscriptionPlan())
                .status(tenant.getStatus())
                .timezone(tenant.getTimezone())
                .language(tenant.getLanguage())
                .logoUrl(tenant.getLogoUrl())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .createdBy(tenant.getCreatedBy())
                .updatedBy(tenant.getUpdatedBy())
                .build();
    }
}
