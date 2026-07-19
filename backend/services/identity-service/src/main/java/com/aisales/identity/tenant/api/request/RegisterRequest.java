package com.aisales.identity.tenant.api.request;

import com.aisales.common.core.validation.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;


@Data
public class RegisterRequest {

    @NotBlank
    @Email
    private String email;

    @PasswordPolicy
    private String password;

    @NotBlank
    private String firstName;

    @NotBlank
    private String lastName;

    @NotBlank
    private String companyName;
}
