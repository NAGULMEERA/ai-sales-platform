package com.aisales.common.security.util;

import com.aisales.common.core.constant.SecurityConstants;
import com.aisales.common.exception.exception.UnauthorizedException;
import com.aisales.common.security.jwt.JwtRsaProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class JwtTokenValidator {

    private final JwtUtils jwtUtils;
    private final JwtRsaProperties jwtRsaProperties;

    public Claims validateAccessToken(String token) {
        return validateToken(token, SecurityConstants.ACCESS_TOKEN);
    }

    public Claims validateRefreshToken(String token) {
        return validateToken(token, SecurityConstants.REFRESH_TOKEN);
    }

    private Claims validateToken(String token, String expectedType) {
        try {
            Claims claims = jwtUtils.parseClaims(token);
            if (jwtUtils.isTokenExpired(claims)) {
                throw new UnauthorizedException("Token has expired");
            }
            String tokenType = claims.get(SecurityConstants.TOKEN_TYPE_CLAIM, String.class);
            if (!expectedType.equals(tokenType)) {
                throw new UnauthorizedException("Invalid token type");
            }
            validateIssuerAudience(claims);
            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid token: " + ex.getMessage());
        }
    }

    private void validateIssuerAudience(Claims claims) {
        String expectedIssuer = jwtRsaProperties.getIssuer();
        String expectedAudience = jwtRsaProperties.getAudience();
        boolean require = jwtRsaProperties.isRequireIssuerAudience();

        String issuer = claims.getIssuer();
        if (require && !StringUtils.hasText(issuer)) {
            throw new UnauthorizedException("Token missing issuer");
        }
        if (StringUtils.hasText(issuer) && StringUtils.hasText(expectedIssuer) && !expectedIssuer.equals(issuer)) {
            throw new UnauthorizedException("Invalid token issuer");
        }

        Set<String> audiences = claims.getAudience();
        boolean hasAudience = audiences != null && !audiences.isEmpty();
        if (require && !hasAudience) {
            throw new UnauthorizedException("Token missing audience");
        }
        if (hasAudience && StringUtils.hasText(expectedAudience) && !audiences.contains(expectedAudience)) {
            throw new UnauthorizedException("Invalid token audience");
        }
    }
}
