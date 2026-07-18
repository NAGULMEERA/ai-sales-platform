package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualifyLeadRequest {

    @Min(0)
    @Max(100)
    private Integer score;

    @Size(max = 2000)
    private String notes;
}
