package com.aisales.identity.application.service;

import com.aisales.common.contracts.auth.AuthResponse;
import com.aisales.common.security.model.TokenInfo;
import com.aisales.identity.domain.entity.RefreshToken;
import com.aisales.identity.domain.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Issues access/refresh tokens after identity has been proven or explicitly granted.
 * <p>
 * Separated from {@link AuthService} so account lifecycle (e.g. registration) is not
 * coupled to authentication. {@link RegistrationService} may skip token issuance when
 * email verification is required before first login.
 */
@Service
@RequiredArgsConstructor
public class TokenService {

    private final RbacService rbacService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;
    private final OpaqueRefreshTokenGenerator opaqueRefreshTokenGenerator;

    public AuthResponse issueTokens(User user, String ipAddress, String userAgent) {
        Set<String> permissions = rbacService.resolvePermissions(user.getRoles());
        TokenInfo tokens = jwtService.generateAccessToken(user, permissions);
        String opaqueRefreshToken = opaqueRefreshTokenGenerator.generate();
        RefreshToken refreshToken = refreshTokenService.createRefreshToken(user.getId(), opaqueRefreshToken);
        sessionService.createSession(user.getId(), refreshToken, ipAddress, userAgent);
        return AuthResponse.builder()
                .accessToken(tokens.getAccessToken())
                .refreshToken(opaqueRefreshToken)
                .tokenType(tokens.getTokenType())
                .expiresAt(tokens.getExpiresAt())
                .userId(user.getId().toString())
                .tenantId(user.getTenantId().toString())
                .roles(user.getRoles())
                .permissions(permissions)
                .build();
    }
}
