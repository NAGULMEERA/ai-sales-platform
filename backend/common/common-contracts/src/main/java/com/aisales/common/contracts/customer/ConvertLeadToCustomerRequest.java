package com.aisales.common.contracts.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConvertLeadToCustomerRequest {

    @NotNull
    private UUID leadId;

    @NotBlank
    @Size(max = 255)
    private String fullName;

    @Size(max = 50)
    private String phone;

    @Email
    @Size(max = 255)
    private String email;

    @Size(max = 50)
    private String whatsapp;

    /** When set, links the lead to this existing customer instead of creating. */
    private UUID existingCustomerId;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
