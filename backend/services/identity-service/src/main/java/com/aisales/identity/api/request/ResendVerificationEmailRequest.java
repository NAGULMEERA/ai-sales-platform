package com.aisales.identity.api.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendVerificationEmailRequest {

    @NotBlank
    @Email
    private String email;
}
