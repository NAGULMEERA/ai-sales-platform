package com.aisales.common.security.util;

import com.aisales.common.core.constant.SecurityConstants;
import com.aisales.common.security.model.TokenInfo;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${aisales.security.jwt.secret:aisales-default-jwt-secret-key-change-in-production}") String secret,
            @Value("${aisales.security.jwt.access-token-expiration-ms:3600000}") long accessTokenExpirationMs,
            @Value("${aisales.security.jwt.refresh-token-expiration-ms:86400000}") long refreshTokenExpirationMs) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public TokenInfo generateTokens(String userId, String tenantId, String email, Set<String> roles) {
        return generateTokens(userId, tenantId, null, email, roles, Set.of());
    }

    public TokenInfo generateTokens(String userId, String tenantId, String organizationId, String email,
                                    Set<String> roles, Set<String> permissions) {
        Instant now = Instant.now();
        Instant accessExpiry = now.plusMillis(accessTokenExpirationMs);
        Instant refreshExpiry = now.plusMillis(refreshTokenExpirationMs);

        String accessToken = buildToken(userId, tenantId, organizationId, email, roles, permissions,
                accessExpiry, SecurityConstants.ACCESS_TOKEN);
        String refreshToken = buildToken(userId, tenantId, organizationId, email, roles, permissions,
                refreshExpiry, SecurityConstants.REFRESH_TOKEN);

        return TokenInfo.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresAt(accessExpiry)
                .scopes(roles)
                .build();
    }

    /**
     * Access token only. Refresh tokens should be opaque values persisted by identity-service.
     */
    public TokenInfo generateAccessToken(String userId, String tenantId, String organizationId, String email,
                                         Set<String> roles, Set<String> permissions) {
        Instant accessExpiry = Instant.now().plusMillis(accessTokenExpirationMs);
        String accessToken = buildToken(userId, tenantId, organizationId, email, roles, permissions,
                accessExpiry, SecurityConstants.ACCESS_TOKEN);
        return TokenInfo.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresAt(accessExpiry)
                .scopes(roles)
                .build();
    }

    private String buildToken(String userId, String tenantId, String organizationId, String email,
                              Set<String> roles, Set<String> permissions, Instant expiry, String tokenType) {
        var builder = Jwts.builder()
                .subject(userId)
                .claim(SecurityConstants.TENANT_ID_CLAIM, tenantId)
                .claim(SecurityConstants.EMAIL_CLAIM, email)
                .claim(SecurityConstants.ROLES_CLAIM, roles)
                .claim(SecurityConstants.PERMISSIONS_CLAIM, permissions)
                .claim(SecurityConstants.TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiry));
        if (organizationId != null) {
            builder.claim(SecurityConstants.ORGANIZATION_ID_CLAIM, organizationId);
        }
        return builder.signWith(secretKey).compact();
    }

    private String buildToken(String userId, String tenantId, String email, Set<String> roles,
                              Instant expiry, String tokenType) {
        return buildToken(userId, tenantId, null, email, roles, Set.of(), expiry, tokenType);
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public Map<String, Object> getTokenClaims(String token, JwtUtils jwtUtils) {
        var claims = jwtUtils.parseClaims(token);
        return Map.of(
                "userId", claims.getSubject(),
                "tenantId", claims.get(SecurityConstants.TENANT_ID_CLAIM),
                "email", claims.get(SecurityConstants.EMAIL_CLAIM),
                "roles", claims.get(SecurityConstants.ROLES_CLAIM));
    }
}
