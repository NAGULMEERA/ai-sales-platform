package com.aisales.common.contracts.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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

    private CustomerSourceType sourceType;

    /** Optional lead reference (no cross-service FK). */
    private UUID sourceLeadId;

    private Map<String, Object> metadata;
}
