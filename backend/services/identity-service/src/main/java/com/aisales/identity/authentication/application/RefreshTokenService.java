package com.aisales.identity.authentication.application;

import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.exception.UnauthorizedException;
import com.aisales.common.exception.model.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aisales.identity.authentication.domain.entity.RefreshToken;
import com.aisales.identity.authentication.infrastructure.persistence.RefreshTokenRepository;
import com.aisales.identity.session.application.SessionService;



/**
 * Opaque refresh tokens with rotation and family-based reuse detection.
 * Each login starts a new token family; rotation keeps the same family id.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final SessionService sessionService;

    @Value("${aisales.security.jwt.refresh-token-expiration-ms:86400000}")
    private long refreshTokenExpirationMs;

    @Transactional
    public RefreshToken createForSession(UUID userId, String token) {
        UUID familyId = UUID.randomUUID();
        RefreshToken refreshToken = RefreshToken.builder()
                .userId(userId)
                .token(token)
                .tokenFamilyId(familyId)
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Rotates a valid refresh token under pessimistic lock. If the token was already
     * revoked (replay after rotation or concurrent refresh loser), revokes the entire
     * family and rejects the request.
     */
    @Transactional
    public RefreshToken rotate(String presentedToken, String newTokenValue) {
        RefreshToken active = refreshTokenRepository.findActiveForUpdate(presentedToken)
                .orElseThrow(() -> handleInvalidOrReuse(presentedToken));
        if (active.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }
        active.setRevoked(true);
        refreshTokenRepository.save(active);
        sessionService.revokeSessionByRefreshTokenId(active.getId());

        RefreshToken rotated = RefreshToken.builder()
                .userId(active.getUserId())
                .token(newTokenValue)
                .tokenFamilyId(active.getTokenFamilyId())
                .expiresAt(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();
        return refreshTokenRepository.save(rotated);
    }

    @Transactional
    public void revokeToken(String token) {
        refreshTokenRepository.findByTokenAndRevokedFalse(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            sessionService.revokeSessionByRefreshTokenId(rt.getId());
        });
    }

    @Transactional
    public void revokeAllForUser(UUID userId) {
        refreshTokenRepository.revokeAllActiveByUserId(userId);
        sessionService.revokeAllSessions(userId);
    }

    @Transactional
    public void revokeTokenFamily(UUID tokenFamilyId) {
        refreshTokenRepository.findByTokenFamilyId(tokenFamilyId).forEach(token -> {
            if (!token.isRevoked()) {
                token.setRevoked(true);
                refreshTokenRepository.save(token);
            }
            sessionService.revokeSessionByRefreshTokenId(token.getId());
        });
    }

    private RuntimeException handleInvalidOrReuse(String token) {
        return refreshTokenRepository.findByToken(token)
                .filter(RefreshToken::isRevoked)
                .map(revoked -> {
                    revokeTokenFamily(revoked.getTokenFamilyId());
                    return new BusinessException(
                            ErrorCode.AUTH_REFRESH_TOKEN_REUSE,
                            "Refresh token reuse detected. All sessions in this chain were revoked.");
                })
                .orElseGet(() -> new UnauthorizedException("Invalid refresh token"));
    }
}
