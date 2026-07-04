package com.aisales.identity.authentication.application;

import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.exception.UnauthorizedException;
import com.aisales.common.exception.model.ErrorCode;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.aisales.identity.authentication.domain.entity.RefreshToken;
import com.aisales.identity.authentication.infrastructure.persistence.RefreshTokenRepository;
import com.aisales.identity.session.application.SessionService;



import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private SessionService sessionService;

    private RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        refreshTokenService = new RefreshTokenService(refreshTokenRepository, sessionService);
        org.springframework.test.util.ReflectionTestUtils.setField(refreshTokenService, "refreshTokenExpirationMs", 86_400_000L);
    }

    @Test
    void shouldRotateActiveRefreshToken() {
        UUID familyId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        RefreshToken active = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .token("old-token")
                .tokenFamilyId(familyId)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findActiveForUpdate("old-token")).thenReturn(Optional.of(active));
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RefreshToken rotated = refreshTokenService.rotate("old-token", "new-token");

        assertThat(active.isRevoked()).isTrue();
        assertThat(rotated.getToken()).isEqualTo("new-token");
        assertThat(rotated.getTokenFamilyId()).isEqualTo(familyId);
        assertThat(rotated.isRevoked()).isFalse();
        verify(sessionService).revokeSessionByRefreshTokenId(active.getId());
    }

    @Test
    void shouldRejectReuseOfRevokedRefreshTokenAndRevokeFamily() {
        UUID familyId = UUID.randomUUID();
        RefreshToken revoked = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .token("reused-token")
                .tokenFamilyId(familyId)
                .expiresAt(Instant.now().plusSeconds(3600))
                .revoked(true)
                .build();
        when(refreshTokenRepository.findActiveForUpdate("reused-token")).thenReturn(Optional.empty());
        when(refreshTokenRepository.findByToken("reused-token")).thenReturn(Optional.of(revoked));
        when(refreshTokenRepository.findByTokenFamilyId(familyId)).thenReturn(List.of(revoked));

        assertThatThrownBy(() -> refreshTokenService.rotate("reused-token", "new-token"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_REUSE);
    }

    @Test
    void shouldRejectExpiredRefreshToken() {
        RefreshToken expired = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(UUID.randomUUID())
                .token("expired-token")
                .tokenFamilyId(UUID.randomUUID())
                .expiresAt(Instant.now().minusSeconds(60))
                .revoked(false)
                .build();
        when(refreshTokenRepository.findActiveForUpdate("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.rotate("expired-token", "new-token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void shouldCreateSessionWithNewTokenFamily() {
        UUID userId = UUID.randomUUID();
        when(refreshTokenRepository.save(any(RefreshToken.class))).thenAnswer(invocation -> {
            RefreshToken saved = invocation.getArgument(0);
            saved.setId(UUID.randomUUID());
            return saved;
        });

        RefreshToken created = refreshTokenService.createForSession(userId, "session-token");

        assertThat(created.getTokenFamilyId()).isNotNull();
        assertThat(created.getUserId()).isEqualTo(userId);
        assertThat(created.isRevoked()).isFalse();
    }

    @Test
    void shouldRevokeAllTokensAndSessionsForUser() {
        UUID userId = UUID.randomUUID();
        refreshTokenService.revokeAllForUser(userId);
        verify(refreshTokenRepository).revokeAllActiveByUserId(userId);
        verify(sessionService).revokeAllSessions(userId);
    }
}
