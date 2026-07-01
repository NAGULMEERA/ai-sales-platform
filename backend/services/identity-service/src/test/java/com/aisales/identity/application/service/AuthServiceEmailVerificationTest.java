package com.aisales.identity.application.service;

import com.aisales.common.contracts.auth.LoginRequest;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.UnauthorizedException;
import com.aisales.identity.domain.entity.User;
import com.aisales.identity.infrastructure.configuration.AuthProperties;
import com.aisales.identity.infrastructure.persistence.EmailVerificationTokenRepository;
import com.aisales.identity.infrastructure.persistence.PasswordResetTokenRepository;
import com.aisales.identity.infrastructure.persistence.RefreshTokenRepository;
import com.aisales.identity.infrastructure.persistence.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceEmailVerificationTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private SessionService sessionService;
    @Mock private TokenService tokenService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EventPublisher eventPublisher;
    @Mock private PasswordResetTokenService passwordResetTokenService;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private AuditService auditService;

    private AuthProperties authProperties;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authProperties.setRequireEmailVerificationForLogin(true);
        authService = new AuthService(
                userRepository,
                refreshTokenRepository,
                emailVerificationTokenRepository,
                passwordResetTokenRepository,
                refreshTokenService,
                sessionService,
                tokenService,
                passwordEncoder,
                eventPublisher,
                passwordResetTokenService,
                emailVerificationService,
                auditService,
                authProperties);
    }

    @Test
    void shouldRejectLoginWhenEmailNotVerified() {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setTenantId(UUID.randomUUID());
        user.setEmail("user@example.com");
        user.setPasswordHash("hash");
        user.setFirstName("Test");
        user.setLastName("User");
        user.setStatus(User.UserStatus.ACTIVE);
        user.setEmailVerified(false);
        user.setRoles(Set.of("TENANT_ADMIN"));

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("Password123!", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login(
                LoginRequest.builder().email("user@example.com").password("Password123!").build(),
                "127.0.0.1",
                "JUnit"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Email verification required");
    }
}
