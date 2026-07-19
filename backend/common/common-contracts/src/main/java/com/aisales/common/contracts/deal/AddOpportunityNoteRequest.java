package com.aisales.common.contracts.deal;

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
public class AddOpportunityNoteRequest {

    @NotBlank
    @Size(max = 4000)
    private String note;
}
