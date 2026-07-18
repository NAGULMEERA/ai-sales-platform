package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Ensures a tenant pipeline exists from a known template code.
 * Same API for every industry — only the template code/metadata differs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EnsurePipelineRequest {

    /**
     * Template code, e.g. DEFAULT_SALES_V1, REAL_ESTATE_SALES_V1, AUTOMOBILE_SALES_V1.
     * Referenced by industry plugin {@code defaultPipelineCode}.
     */
    @NotBlank
    @Size(max = 100)
    private String code;

    /**
     * When true and no default exists yet, mark this pipeline as the tenant default.
     * Ignored if the pipeline already exists.
     */
    @Builder.Default
    private boolean makeDefault = false;
}
