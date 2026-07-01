package com.aisales.identity.application.service;

import com.aisales.identity.domain.entity.RefreshToken;
import com.aisales.identity.infrastructure.persistence.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${aisales.security.jwt.refresh-token-expiration-ms:86400000}")
    private long refreshTokenExpirationMs;

    @Transactional
    public RefreshToken createRefreshToken(UUID userId, String token) {
        refreshTokenRepository.deleteByUserId(userId);
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByTokenAndRevokedFalse(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }
}
