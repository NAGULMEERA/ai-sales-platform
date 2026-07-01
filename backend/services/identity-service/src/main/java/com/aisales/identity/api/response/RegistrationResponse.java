package com.aisales.identity.api.response;

import com.aisales.common.contracts.auth.AuthResponse;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class RegistrationResponse {

    private UUID tenantId;
    private UUID userId;
    private String companyName;
    private String tenantSlug;
    private String email;
    private boolean emailVerificationRequired;
    private String message;

    /**
     * Present only when {@code aisales.auth.auto-login-after-register=true}.
     * Registration creates the account; token issuance is optional and independent.
     */
    private AuthResponse authentication;
}
