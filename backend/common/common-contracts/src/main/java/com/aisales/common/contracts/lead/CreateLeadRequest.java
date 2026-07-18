package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLeadRequest {

    /** Optional tenant pipeline. When null, the tenant default sales pipeline is used. */
    private UUID pipelineId;

    @NotBlank
    @Size(max = 255)
    private String customerName;

    @NotBlank
    @Size(max = 50)
    private String phone;

    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 50)
    private String sourceType;

    @Size(max = 255)
    private String sourceId;

    @Size(max = 255)
    private String campaign;

    /**
     * Industry-agnostic lead attributes (plugin-defined keys).
     * Real Estate example: budget, location, propertyType.
     * Automobile example: vehicle, budget, financeRequired.
     * Stored in leads.metadata — no industry columns on Lead.
     */
    @Builder.Default
    private Map<String, Object> attributes = new HashMap<>();
}
