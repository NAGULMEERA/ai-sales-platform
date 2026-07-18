package com.aisales.common.contracts.lead;

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
public class CreateLeadNoteRequest {

    @NotBlank
    @Size(max = 10000)
    private String note;

    @Size(max = 50)
    private String noteType;
}
