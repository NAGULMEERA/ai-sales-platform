package com.aisales.common.contracts.deal;

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
public class UpdateOpportunityStageRequest {

    @NotNull
    private OpportunityStatus status;

    @Size(max = 2000)
    private String reason;
}
