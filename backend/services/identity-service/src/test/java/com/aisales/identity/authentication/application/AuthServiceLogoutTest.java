package com.aisales.identity.authentication.application;

import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.UnauthorizedException;
import com.aisales.common.observability.metrics.PlatformMetrics;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.authentication.api.request.LogoutRequest;
import com.aisales.identity.authentication.domain.entity.RefreshToken;
import com.aisales.identity.authentication.infrastructure.configuration.AuthProperties;
import com.aisales.identity.authentication.infrastructure.persistence.EmailVerificationTokenRepository;
import com.aisales.identity.authentication.infrastructure.persistence.PasswordResetTokenRepository;
import com.aisales.identity.authentication.infrastructure.persistence.RefreshTokenRepository;
import com.aisales.identity.session.application.SessionService;
import com.aisales.identity.user.infrastructure.persistence.UserRepository;



import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLogoutTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private SessionService sessionService;
    @Mock private TokenService tokenService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordResetTokenService passwordResetTokenService;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private LoginLockoutService loginLockoutService;
    @Mock private AuditService auditService;
    @Mock private EventPublisher eventPublisher;
    @Mock private ObjectProvider<PlatformMetrics> platformMetrics;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                emailVerificationTokenRepository,
                passwordResetTokenRepository,
                refreshTokenService,
                sessionService,
                tokenService,
                passwordEncoder,
                passwordResetTokenService,
                emailVerificationService,
                loginLockoutService,
                auditService,
                eventPublisher,
                authProperties,
                platformMetrics);
    }

    private AuthProperties authProperties;

    @Test
    void shouldRejectLogoutWithRefreshTokenOwnedByAnotherUser() {
        UUID authenticatedUser = UUID.randomUUID();
        UUID tokenOwner = UUID.randomUUID();
        RefreshToken token = RefreshToken.builder()
                .id(UUID.randomUUID())
                .userId(tokenOwner)
                .token("refresh")
                .tokenFamilyId(UUID.randomUUID())
                .revoked(false)
                .build();
        when(refreshTokenRepository.findByTokenAndRevokedFalse("refresh")).thenReturn(Optional.of(token));

        LogoutRequest request = new LogoutRequest();
        request.setRefreshToken("refresh");
        assertThatThrownBy(() -> authService.logout(request, authenticatedUser, "127.0.0.1", "JUnit"))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void shouldLogoutAllDevices() {
        UUID userId = UUID.randomUUID();
        authService.logoutAllDevices(userId, "127.0.0.1", "JUnit");
        verify(refreshTokenService).revokeAllForUser(userId);
    }
}
