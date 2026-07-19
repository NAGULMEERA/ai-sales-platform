package com.aisales.common.contracts.deal;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CloseOpportunityRequest {

    @Size(max = 2000)
    private String reason;
}
