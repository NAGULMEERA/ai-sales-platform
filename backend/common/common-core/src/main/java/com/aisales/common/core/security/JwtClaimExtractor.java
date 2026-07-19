package com.aisales.common.core.security;

import com.aisales.common.core.constant.SecurityConstants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;

import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Stateless JWT parsing for gateway and other non-servlet components (RSA / RS256).
 */
public final class JwtClaimExtractor {

    private final RSAPublicKey publicKey;
    private final String expectedIssuer;
    private final String expectedAudience;
    private final boolean requireIssuerAudience;

    public JwtClaimExtractor(RSAPublicKey publicKey) {
        this(publicKey, null, null, false);
    }

    public JwtClaimExtractor(String publicKeyPemOrLocation) {
        this(PemKeyLoader.loadPublicKey(publicKeyPemOrLocation), null, null, false);
    }

    public JwtClaimExtractor(
            String publicKeyPemOrLocation,
            String expectedIssuer,
            String expectedAudience,
            boolean requireIssuerAudience) {
        this(PemKeyLoader.loadPublicKey(publicKeyPemOrLocation), expectedIssuer, expectedAudience, requireIssuerAudience);
    }

    public JwtClaimExtractor(
            RSAPublicKey publicKey,
            String expectedIssuer,
            String expectedAudience,
            boolean requireIssuerAudience) {
        this.publicKey = publicKey;
        this.expectedIssuer = expectedIssuer;
        this.expectedAudience = expectedAudience;
        this.requireIssuerAudience = requireIssuerAudience;
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
        validateIssuerAudience(claims);
        return claims;
    }

    private void validateIssuerAudience(Claims claims) {
        String issuer = claims.getIssuer();
        if (requireIssuerAudience && (issuer == null || issuer.isBlank())) {
            throw new JwtException("Token missing issuer");
        }
        if (issuer != null && !issuer.isBlank() && expectedIssuer != null && !expectedIssuer.isBlank()
                && !expectedIssuer.equals(issuer)) {
            throw new JwtException("Invalid token issuer");
        }

        Set<String> audiences = claims.getAudience();
        boolean hasAudience = audiences != null && !audiences.isEmpty();
        if (requireIssuerAudience && !hasAudience) {
            throw new JwtException("Token missing audience");
        }
        if (hasAudience && expectedAudience != null && !expectedAudience.isBlank()
                && !audiences.contains(expectedAudience)) {
            throw new JwtException("Invalid token audience");
        }
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
