package com.aisales.common.contracts.tenant;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateTenantRequest {

    @Size(max = 100)
    private String name;

    private SubscriptionPlan subscriptionPlan;

    @Size(max = 64)
    private String timezone;

    @Size(max = 10)
    private String language;

    @Size(max = 512)
    private String logoUrl;
}
