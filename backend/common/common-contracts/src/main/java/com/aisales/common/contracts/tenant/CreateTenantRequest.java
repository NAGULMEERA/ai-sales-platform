package com.aisales.common.contracts.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTenantRequest {

    @NotBlank(message = "Tenant name cannot be empty")
    @Size(max = 100)
    private String name;

    @Size(max = 50)
    @Pattern(regexp = "^[a-z0-9]+(?:-[a-z0-9]+)*$", message = "Slug must contain lowercase letters, numbers, and hyphens only")
    private String slug;

    @Size(max = 50)
    @Pattern(regexp = "^[A-Z0-9_]*$", message = "Tenant code must contain uppercase letters, numbers, and underscores only")
    private String tenantCode;

    @NotNull(message = "Industry is required")
    private TenantIndustry industry;

    private SubscriptionPlan subscriptionPlan;

    @Size(max = 64)
    private String timezone;

    @Size(max = 10)
    private String language;

    @Size(max = 512)
    private String logoUrl;
}
