package com.aisales.identity.authentication.application;

import com.aisales.common.contracts.auth.AuthResponse;
import com.aisales.common.security.model.TokenInfo;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.aisales.identity.authentication.domain.entity.RefreshToken;
import com.aisales.identity.authorization.application.RbacService;
import com.aisales.identity.session.application.SessionService;
import com.aisales.identity.tenant.application.RegistrationService;
import com.aisales.identity.user.domain.entity.User;



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
        String opaqueRefreshToken = opaqueRefreshTokenGenerator.generate();
        RefreshToken refreshToken = refreshTokenService.createForSession(user.getId(), opaqueRefreshToken);
        return buildAuthResponse(user, refreshToken, opaqueRefreshToken, ipAddress, userAgent);
    }

    public AuthResponse rotateTokens(User user, String presentedRefreshToken, String ipAddress, String userAgent) {
        String opaqueRefreshToken = opaqueRefreshTokenGenerator.generate();
        RefreshToken refreshToken = refreshTokenService.rotate(presentedRefreshToken, opaqueRefreshToken);
        return buildAuthResponse(user, refreshToken, opaqueRefreshToken, ipAddress, userAgent);
    }

    private AuthResponse buildAuthResponse(User user, RefreshToken refreshToken, String opaqueRefreshToken,
                                           String ipAddress, String userAgent) {
        Set<String> permissions = rbacService.resolvePermissions(user.getRoles());
        TokenInfo tokens = jwtService.generateAccessToken(user, permissions);
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
