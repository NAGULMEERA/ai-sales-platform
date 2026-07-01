package com.aisales.common.contracts.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Instant expiresAt;
    private String userId;
    private String tenantId;
    private Set<String> roles;
    private Set<String> permissions;
}
