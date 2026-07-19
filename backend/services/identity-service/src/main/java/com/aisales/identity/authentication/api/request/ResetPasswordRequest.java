package com.aisales.identity.authentication.api.request;

import com.aisales.common.core.validation.PasswordPolicy;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResetPasswordRequest {

    @NotBlank
    private String token;

    @PasswordPolicy
    private String newPassword;
}
