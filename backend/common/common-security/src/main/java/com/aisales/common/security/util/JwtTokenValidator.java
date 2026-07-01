package com.aisales.common.security.util;

import com.aisales.common.core.constant.SecurityConstants;
import com.aisales.common.exception.exception.UnauthorizedException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtTokenValidator {

    private final JwtUtils jwtUtils;

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
            return claims;
        } catch (JwtException | IllegalArgumentException ex) {
            throw new UnauthorizedException("Invalid token: " + ex.getMessage());
        }
    }
}
