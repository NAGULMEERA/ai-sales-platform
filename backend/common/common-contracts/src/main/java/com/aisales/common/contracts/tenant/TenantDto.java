package com.aisales.common.contracts.tenant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TenantDto {

    private UUID id;
    private String tenantCode;
    private String slug;
    private String name;
    private TenantIndustry industry;
    private SubscriptionPlan subscriptionPlan;
    private TenantStatus status;
    private String timezone;
    private String language;
    private String logoUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String updatedBy;
}
