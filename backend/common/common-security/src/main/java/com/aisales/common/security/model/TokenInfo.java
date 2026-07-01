package com.aisales.common.security.model;

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
public class TokenInfo {

    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Instant expiresAt;
    private Set<String> scopes;
}
