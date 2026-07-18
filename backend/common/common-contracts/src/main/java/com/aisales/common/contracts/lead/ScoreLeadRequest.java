package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScoreLeadRequest {

    @NotNull
    @Min(0)
    @Max(100)
    private Integer score;

    @NotBlank
    @Size(max = 50)
    private String scoreType;

    @Size(max = 2000)
    private String explanation;
}
