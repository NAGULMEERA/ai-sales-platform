package com.aisales.identity.api.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {

    @NotBlank
    private String refreshToken;
}
