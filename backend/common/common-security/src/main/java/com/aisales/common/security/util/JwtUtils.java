package com.aisales.common.security.util;

import com.aisales.common.core.constant.SecurityConstants;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.security.jwt.PlatformRsaKeyProvider;
import com.aisales.common.security.model.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtUtils {

    private final PlatformRsaKeyProvider keyProvider;

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(keyProvider.getPublicKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUserId(Claims claims) {
        return claims.getSubject();
    }

    public String extractTenantId(Claims claims) {
        return claims.get(SecurityConstants.TENANT_ID_CLAIM, String.class);
    }

    @SuppressWarnings("unchecked")
    public Set<String> extractRoles(Claims claims) {
        List<String> roles = claims.get(SecurityConstants.ROLES_CLAIM, List.class);
        return roles != null ? new HashSet<>(roles) : Set.of();
    }

    public boolean isTokenExpired(Claims claims) {
        Date expiration = claims.getExpiration();
        return expiration != null && expiration.before(new Date());
    }

    public UserPrincipal toUserPrincipal(Claims claims) {
        String tenantId = extractTenantId(claims);
        TenantContext.setTenantId(tenantId);
        return UserPrincipal.builder()
                .userId(extractUserId(claims))
                .tenantId(tenantId)
                .email(claims.get(SecurityConstants.EMAIL_CLAIM, String.class))
                .roles(extractRoles(claims))
                .enabled(true)
                .build();
    }
}
