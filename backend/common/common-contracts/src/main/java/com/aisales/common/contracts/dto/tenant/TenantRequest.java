package com.aisales.common.contracts.dto.tenant;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class TenantRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String slug;
}
