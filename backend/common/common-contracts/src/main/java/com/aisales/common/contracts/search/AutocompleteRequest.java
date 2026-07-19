package com.aisales.common.contracts.search;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AutocompleteRequest {

    @NotBlank
    @Size(max = 200)
    private String prefix;

    @Builder.Default
    private SearchEntityType entityType = SearchEntityType.ALL;

    @Min(1)
    @Max(20)
    @Builder.Default
    private int limit = 8;
}
