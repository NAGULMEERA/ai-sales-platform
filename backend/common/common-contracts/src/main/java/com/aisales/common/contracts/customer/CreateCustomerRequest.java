package com.aisales.common.contracts.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
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
public class CreateCustomerRequest {

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

    @Size(max = 255)
    private String externalCrmId;

    @Size(max = 255)
    private String governmentId;

    @Size(max = 30)
    private String gender;

    private LocalDate dateOfBirth;

    @Size(max = 20)
    private String language;

    @Size(max = 50)
    private String preferredChannel;

    private CustomerSourceType sourceType;

    /** Optional lead reference (no cross-service FK). */
    private UUID sourceLeadId;

    @Builder.Default
    private Map<String, Object> preferences = new HashMap<>();

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
