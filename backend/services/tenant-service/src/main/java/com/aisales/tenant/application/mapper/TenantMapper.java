package com.aisales.tenant.application.mapper;

import com.aisales.common.contracts.tenant.TenantDto;
import com.aisales.tenant.domain.entity.Tenant;
import org.springframework.stereotype.Component;

@Component
public class TenantMapper {

    public TenantDto toDto(Tenant tenant) {
        return TenantDto.builder()
                .id(tenant.getId())
                .name(tenant.getName())
                .slug(tenant.getSlug())
                .plan(tenant.getPlan())
                .status(tenant.getStatus().name())
                .createdAt(tenant.getCreatedAt())
                .updatedAt(tenant.getUpdatedAt())
                .build();
    }
}
