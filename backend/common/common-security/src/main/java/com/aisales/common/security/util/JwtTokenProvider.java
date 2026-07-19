package com.aisales.common.security.util;

import com.aisales.common.core.constant.SecurityConstants;
import com.aisales.common.security.jwt.JwtRsaProperties;
import com.aisales.common.security.jwt.PlatformRsaKeyProvider;
import com.aisales.common.security.model.TokenInfo;
import io.jsonwebtoken.Jwts;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Mints platform access tokens using RSA (RS256). Enabled only where
 * {@code aisales.security.jwt.signing-enabled=true} (identity-service).
 */
@Component
@ConditionalOnProperty(name = "aisales.security.jwt.signing-enabled", havingValue = "true")
public class JwtTokenProvider {

    private final PlatformRsaKeyProvider keyProvider;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(PlatformRsaKeyProvider keyProvider, JwtRsaProperties properties) {
        keyProvider.requirePrivateKey();
        this.keyProvider = keyProvider;
        this.accessTokenExpirationMs = properties.getAccessTokenExpirationMs();
        this.refreshTokenExpirationMs = properties.getRefreshTokenExpirationMs();
    }

    public TokenInfo generateTokens(String userId, String tenantId, String email, Set<String> roles) {
        return generateTokens(userId, tenantId, null, email, roles, Set.of(), null);
    }

    public TokenInfo generateTokens(String userId, String tenantId, String organizationId, String email,
                                    Set<String> roles, Set<String> permissions) {
        return generateTokens(userId, tenantId, organizationId, email, roles, permissions, null);
    }

    public TokenInfo generateTokens(String userId, String tenantId, String organizationId, String email,
                                    Set<String> roles, Set<String> permissions, String subscriptionPlan) {
        Instant now = Instant.now();
        Instant accessExpiry = now.plusMillis(accessTokenExpirationMs);
        Instant refreshExpiry = now.plusMillis(refreshTokenExpirationMs);

        String accessToken = buildToken(userId, tenantId, organizationId, email, roles, permissions,
                subscriptionPlan, accessExpiry, SecurityConstants.ACCESS_TOKEN);
        String refreshToken = buildToken(userId, tenantId, organizationId, email, roles, permissions,
                subscriptionPlan, refreshExpiry, SecurityConstants.REFRESH_TOKEN);

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
        return generateAccessToken(userId, tenantId, organizationId, email, roles, permissions, null);
    }

    public TokenInfo generateAccessToken(String userId, String tenantId, String organizationId, String email,
                                         Set<String> roles, Set<String> permissions, String subscriptionPlan) {
        Instant accessExpiry = Instant.now().plusMillis(accessTokenExpirationMs);
        String accessToken = buildToken(userId, tenantId, organizationId, email, roles, permissions,
                subscriptionPlan, accessExpiry, SecurityConstants.ACCESS_TOKEN);
        return TokenInfo.builder()
                .accessToken(accessToken)
                .tokenType("Bearer")
                .expiresAt(accessExpiry)
                .scopes(roles)
                .build();
    }

    private String buildToken(String userId, String tenantId, String organizationId, String email,
                              Set<String> roles, Set<String> permissions, String subscriptionPlan,
                              Instant expiry, String tokenType) {
        var builder = Jwts.builder()
                .header().keyId(keyProvider.getKeyId()).and()
                .subject(userId)
                .claim(SecurityConstants.TENANT_ID_CLAIM, tenantId)
                .claim(SecurityConstants.EMAIL_CLAIM, email)
                .claim(SecurityConstants.ROLES_CLAIM, roles)
                .claim(SecurityConstants.PERMISSIONS_CLAIM, permissions)
                .claim(SecurityConstants.TOKEN_TYPE_CLAIM, tokenType)
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiry))
                .signWith(keyProvider.getPrivateKey(), Jwts.SIG.RS256);
        if (organizationId != null) {
            builder.claim(SecurityConstants.ORGANIZATION_ID_CLAIM, organizationId);
        }
        if (StringUtils.hasText(subscriptionPlan)) {
            builder.claim(SecurityConstants.SUBSCRIPTION_PLAN_CLAIM, subscriptionPlan.trim().toUpperCase());
        }
        return builder.compact();
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
