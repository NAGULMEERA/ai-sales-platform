package com.aisales.identity.authentication.application;

import com.aisales.common.security.model.TokenInfo;
import com.aisales.common.security.util.JwtTokenProvider;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.aisales.identity.user.domain.entity.User;



@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtTokenProvider jwtTokenProvider;

    public TokenInfo generateTokens(User user, Set<String> permissions) {
        String organizationId = user.getOrganizationId() != null ? user.getOrganizationId().toString() : null;
        return jwtTokenProvider.generateTokens(
                user.getId().toString(),
                user.getTenantId().toString(),
                organizationId,
                user.getEmail(),
                user.getRoles(),
                permissions);
    }

    public TokenInfo generateAccessToken(User user, Set<String> permissions) {
        String organizationId = user.getOrganizationId() != null ? user.getOrganizationId().toString() : null;
        return jwtTokenProvider.generateAccessToken(
                user.getId().toString(),
                user.getTenantId().toString(),
                organizationId,
                user.getEmail(),
                user.getRoles(),
                permissions);
    }
}
