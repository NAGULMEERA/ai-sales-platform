package com.aisales.common.contracts.lead;

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
public class ChangeLeadStatusRequest {

    @NotNull
    private LeadStatus status;

    @Size(max = 2000)
    private String reason;
}
