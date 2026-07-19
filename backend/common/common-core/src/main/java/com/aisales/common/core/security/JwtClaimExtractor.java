package com.aisales.common.core.security;

import com.aisales.common.core.constant.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;

/**
 * Stateless JWT parsing for gateway and other non-servlet components (RSA / RS256).
 */
public final class JwtClaimExtractor {

    private final RSAPublicKey publicKey;

    public JwtClaimExtractor(RSAPublicKey publicKey) {
        this.publicKey = publicKey;
    }

    public JwtClaimExtractor(String publicKeyPemOrLocation) {
        this(PemKeyLoader.loadPublicKey(publicKeyPemOrLocation));
    }

    public Claims parseAndValidateAccessToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(publicKey)
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

    public static String extractSubscriptionPlan(Claims claims) {
        return claims.get(SecurityConstants.SUBSCRIPTION_PLAN_CLAIM, String.class);
    }

    @SuppressWarnings("unchecked")
    public static boolean hasRole(Claims claims, String role) {
        List<String> roles = claims.get(SecurityConstants.ROLES_CLAIM, List.class);
        return roles != null && roles.contains(role);
    }
}
