package com.aisales.identity.application.service;

import com.aisales.identity.domain.entity.RefreshToken;
import com.aisales.identity.domain.entity.UserSession;
import com.aisales.identity.infrastructure.persistence.UserSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SessionService {

    private final UserSessionRepository userSessionRepository;

    @Value("${aisales.security.jwt.refresh-token-expiration-ms:86400000}")
    private long refreshTokenExpirationMs;

    @Transactional
    public UserSession createSession(UUID userId, RefreshToken refreshToken, String ipAddress, String userAgent) {
        UserSession session = UserSession.builder()
                .userId(userId)
                .refreshTokenId(refreshToken.getId())
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .deviceLabel(parseDeviceLabel(userAgent))
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .createdAt(Instant.now())
                .build();
        return userSessionRepository.save(session);
    }

    @Transactional
    public void revokeSessionByRefreshTokenId(UUID refreshTokenId) {
        userSessionRepository.findByRefreshTokenIdAndRevokedAtIsNull(refreshTokenId)
                .forEach(s -> s.setRevokedAt(Instant.now()));
    }

    @Transactional
    public void revokeAllSessions(UUID userId) {
        userSessionRepository.findByUserIdAndRevokedAtIsNull(userId)
                .forEach(s -> s.setRevokedAt(Instant.now()));
    }

    @Transactional(readOnly = true)
    public List<UserSession> listActiveSessions(UUID userId) {
        return userSessionRepository.findByUserIdAndRevokedAtIsNull(userId);
    }

    private static String parseDeviceLabel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return "Unknown device";
        }
        if (userAgent.length() > 80) {
            return userAgent.substring(0, 80);
        }
        return userAgent;
    }
}
