package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.NotBlank;
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
public class CreateLeadCustomFieldRequest {

    @NotBlank
    @Size(max = 100)
    private String fieldName;

    @NotBlank
    @Size(max = 50)
    private String fieldType;

    private Map<String, Object> fieldOptions;

    private Boolean required;

    private Integer displayOrder;
}
