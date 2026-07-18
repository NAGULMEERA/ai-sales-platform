package com.aisales.common.contracts.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCatalogProductRequest {

    @NotBlank
    @Size(max = 64)
    private String code;

    @NotBlank
    @Size(max = 255)
    private String name;

    @Size(max = 4000)
    private String description;

    @NotNull
    private CatalogProductType productType;

    @Size(max = 100)
    private String category;

    private CatalogItemStatus status;

    private Map<String, Object> attributes;
}
