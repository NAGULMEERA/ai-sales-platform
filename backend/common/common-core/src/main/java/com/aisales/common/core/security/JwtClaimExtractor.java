package com.aisales.common.core.security;

import com.aisales.common.core.constant.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * Stateless JWT parsing for gateway and other non-servlet components.
 */
public final class JwtClaimExtractor {

    private final SecretKey secretKey;

    public JwtClaimExtractor(String secret) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public Claims parseAndValidateAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Date expiration = claims.getExpiration();
        if (expiration != null && expiration.before(new Date())) {
            throw new JwtException("Token has expired");
        }

        String tokenType = claims.get(SecurityConstants.TOKEN_TYPE_CLAIM, String.class);
        if (!SecurityConstants.ACCESS_TOKEN.equals(tokenType)) {
            throw new JwtException("Invalid token type");
        }
        return claims;
    }

    public static String extractTenantId(Claims claims) {
        return claims.get(SecurityConstants.TENANT_ID_CLAIM, String.class);
    }

    public static String extractUserId(Claims claims) {
        return claims.getSubject();
    }

    public static String extractOrganizationId(Claims claims) {
        return claims.get(SecurityConstants.ORGANIZATION_ID_CLAIM, String.class);
    }

    @SuppressWarnings("unchecked")
    public static boolean hasRole(Claims claims, String role) {
        List<String> roles = claims.get(SecurityConstants.ROLES_CLAIM, List.class);
        return roles != null && roles.contains(role);
    }
}
