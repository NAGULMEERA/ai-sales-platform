package com.aisales.identity.authentication.application;

import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.authentication.infrastructure.configuration.AuthProperties;
import com.aisales.identity.user.domain.entity.User;
import com.aisales.identity.user.infrastructure.persistence.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginLockoutServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AuditService auditService;

    private AuthProperties authProperties;
    private LoginLockoutService loginLockoutService;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authProperties.setMaxFailedLoginAttempts(3);
        authProperties.setLockoutDurationMinutes(15);
        loginLockoutService = new LoginLockoutService(userRepository, authProperties, auditService);
    }

    @Test
    void shouldLockAccountWhenMaxFailedAttemptsReached() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .tenantId(UUID.randomUUID())
                .email("a@b.com")
                .passwordHash("hash")
                .firstName("A")
                .lastName("B")
                .status(User.UserStatus.ACTIVE)
                .failedLoginAttempts(2)
                .build();
        user.setId(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        loginLockoutService.recordFailedPasswordAttempt(userId, "127.0.0.1", "JUnit");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(User.UserStatus.LOCKED);
        assertThat(captor.getValue().getFailedLoginAttempts()).isEqualTo(3);
        assertThat(captor.getValue().getLockedUntil()).isAfter(Instant.now());
        verify(auditService).logSecurityEventIndependent(
                eq(user.getTenantId()),
                eq(userId),
                eq(AuditAction.ACCOUNT_LOCKED),
                eq("user"),
                eq(userId.toString()),
                eq("127.0.0.1"),
                eq("JUnit"),
                any());
    }

    @Test
    void shouldUnlockWhenLockExpired() {
        User user = User.builder()
                .status(User.UserStatus.LOCKED)
                .failedLoginAttempts(5)
                .lockedUntil(Instant.now().minusSeconds(60))
                .build();
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        loginLockoutService.unlockIfExpired(user);

        assertThat(user.getStatus()).isEqualTo(User.UserStatus.ACTIVE);
        assertThat(user.getFailedLoginAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void shouldDetectActiveTimedLock() {
        User user = User.builder()
                .status(User.UserStatus.LOCKED)
                .lockedUntil(Instant.now().plusSeconds(300))
                .build();

        assertThat(loginLockoutService.isCurrentlyLocked(user)).isTrue();
    }
}
