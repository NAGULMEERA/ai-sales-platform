package com.aisales.common.contracts.customer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateCustomerRequest {

    @Size(max = 255)
    private String fullName;

    @Size(max = 50)
    private String phone;

    @Email
    @Size(max = 255)
    private String email;

    private CustomerStatus status;

    private Map<String, Object> metadata;
}
