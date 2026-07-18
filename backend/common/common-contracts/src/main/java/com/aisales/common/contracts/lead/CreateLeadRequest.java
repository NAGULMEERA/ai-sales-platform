package com.aisales.common.contracts.lead;

import jakarta.validation.constraints.Email;
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
public class CreateLeadRequest {

    @NotBlank
    @Size(max = 255)
    private String customerName;

    @NotBlank
    @Size(max = 50)
    private String phone;

    @Email
    @Size(max = 255)
    private String email;

    @NotBlank
    @Size(max = 50)
    private String sourceType;

    @Size(max = 255)
    private String sourceId;

    @Size(max = 255)
    private String campaign;
}
