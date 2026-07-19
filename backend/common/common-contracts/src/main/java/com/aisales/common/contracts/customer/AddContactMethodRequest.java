package com.aisales.common.contracts.customer;

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
public class AddContactMethodRequest {

    @NotNull
    private ContactMethodType methodType;

    @NotBlank
    @Size(max = 255)
    private String value;

    @Size(max = 100)
    private String label;

    private Boolean primary;
}
