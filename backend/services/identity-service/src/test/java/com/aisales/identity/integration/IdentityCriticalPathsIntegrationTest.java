package com.aisales.identity.integration;

import com.aisales.common.contracts.auth.LoginRequest;
import com.aisales.common.contracts.auth.RefreshTokenRequest;
import com.aisales.common.contracts.user.CreateUserRequest;
import com.aisales.common.events.outbox.OutboxEvent;
import com.aisales.common.events.outbox.OutboxRepository;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.exception.UnauthorizedException;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.audit.infrastructure.persistence.AuditLogRepository;
import com.aisales.identity.authentication.api.request.ForgotPasswordRequest;
import com.aisales.identity.authentication.api.request.LogoutRequest;
import com.aisales.identity.authentication.api.request.ResetPasswordRequest;
import com.aisales.identity.authentication.api.request.VerifyEmailRequest;
import com.aisales.identity.authentication.application.AuthService;
import com.aisales.identity.authentication.domain.entity.EmailVerificationToken;
import com.aisales.identity.authentication.domain.entity.PasswordResetToken;
import com.aisales.identity.authentication.infrastructure.persistence.EmailVerificationTokenRepository;
import com.aisales.identity.authentication.infrastructure.persistence.PasswordResetTokenRepository;
import com.aisales.identity.authentication.infrastructure.persistence.RefreshTokenRepository;
import com.aisales.identity.tenant.api.request.RegisterRequest;
import com.aisales.identity.tenant.api.response.RegistrationResponse;
import com.aisales.identity.tenant.application.RegistrationService;
import com.aisales.identity.user.application.UserService;
import com.aisales.identity.user.infrastructure.persistence.UserRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityCriticalPathsIntegrationTest extends IdentityIntegrationTestBase {

    @Autowired private RegistrationService registrationService;
    @Autowired private AuthService authService;
    @Autowired private AuditLogRepository auditLogRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private RefreshTokenRepository refreshTokenRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Autowired private OutboxRepository outboxRepository;
    @Autowired private UserService userService;

    @Test
    void shouldRegisterLoginLogoutAndAuditSecurityEvents() {
        var registration = registerUser("flow@example.com", "Flow Corp");
        verifyUserEmail(registration.getUserId());

        var login = authService.login(
                LoginRequest.builder().email("flow@example.com").password("Password123!").build(),
                "127.0.0.1", "JUnit");
        assertThat(login.getRefreshToken()).isNotBlank();

        LogoutRequest logoutRequest = new LogoutRequest();
        logoutRequest.setRefreshToken(login.getRefreshToken());
        authService.logout(logoutRequest, registration.getUserId(), "127.0.0.1", "JUnit");

        assertThat(auditLogRepository.countByAction(AuditAction.USER_REGISTERED.name())).isGreaterThanOrEqualTo(1);
        assertThat(auditLogRepository.countByAction(AuditAction.USER_LOGIN.name())).isGreaterThanOrEqualTo(1);
        assertThat(auditLogRepository.countByAction(AuditAction.USER_LOGOUT.name())).isGreaterThanOrEqualTo(1);
        assertThat(refreshTokenRepository.findByTokenAndRevokedFalse(
                        com.aisales.identity.authentication.application.RefreshTokenHasher.sha256Hex(
                                login.getRefreshToken())))
                .isEmpty();
    }

    @Test
    void shouldAuditLoginFailureWithoutStoringPassword() {
        assertThatThrownBy(() -> authService.login(
                LoginRequest.builder().email("missing@example.com").password("Password123!").build(),
                "127.0.0.1", "JUnit"))
                .isInstanceOf(UnauthorizedException.class);

        assertThat(auditLogRepository.countByAction(AuditAction.LOGIN_FAILED.name())).isGreaterThanOrEqualTo(1);
        assertThat(outboxRepository.findByStatus(OutboxEvent.OutboxStatus.PENDING))
                .extracting(OutboxEvent::getEventType)
                .contains("AuditRecorded");
    }

    @Test
    void shouldLockAccountAfterRepeatedFailedLogins() {
        var registration = registerUser("lockout@example.com", "Lockout Corp");
        verifyUserEmail(registration.getUserId());

        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> authService.login(
                    LoginRequest.builder().email("lockout@example.com").password("WrongPassword1!").build(),
                    "127.0.0.1", "JUnit"))
                    .isInstanceOf(UnauthorizedException.class);
        }

        var user = userRepository.findById(registration.getUserId()).orElseThrow();
        assertThat(user.getStatus()).isEqualTo(com.aisales.identity.user.domain.entity.User.UserStatus.LOCKED);
        assertThat(user.getLockedUntil()).isNotNull();
        assertThat(auditLogRepository.countByAction(AuditAction.ACCOUNT_LOCKED.name())).isGreaterThanOrEqualTo(1);

        assertThatThrownBy(() -> authService.login(
                LoginRequest.builder().email("lockout@example.com").password("Password123!").build(),
                "127.0.0.1", "JUnit"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("locked");
    }

    @Test
    void shouldPersistRegistrationEventsInOutbox() {
        registerUser("outbox@example.com", "Outbox Corp");
        // TenantCreated + UserCreated + EmailVerificationRequested (+ AuditRecorded rows)
        assertThat(outboxRepository.findByStatus(OutboxEvent.OutboxStatus.PENDING)).hasSizeGreaterThanOrEqualTo(3);
        assertThat(outboxRepository.findByStatus(OutboxEvent.OutboxStatus.PENDING))
                .extracting(OutboxEvent::getEventType)
                .contains("EmailVerificationRequested", "AuditRecorded");
    }

    @Test
    void shouldRotateRefreshTokenAndDetectReuse() {
        var registration = registerUser("jwt@example.com", "Jwt Corp");
        verifyUserEmail(registration.getUserId());

        var initial = authService.login(
                LoginRequest.builder().email("jwt@example.com").password("Password123!").build(),
                "127.0.0.1", "JUnit");
        String oldRefresh = initial.getRefreshToken();

        var rotated = authService.refresh(
                RefreshTokenRequest.builder().refreshToken(oldRefresh).build(),
                "127.0.0.1", "JUnit");
        assertThat(rotated.getRefreshToken()).isNotEqualTo(oldRefresh);

        assertThatThrownBy(() -> authService.refresh(
                RefreshTokenRequest.builder().refreshToken(oldRefresh).build(),
                "127.0.0.1", "JUnit"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldResetPasswordRevokeSessionsAndAuditEvent() {
        var registration = registerUser("reset@example.com", "Reset Corp");
        verifyUserEmail(registration.getUserId());

        ForgotPasswordRequest forgotRequest = new ForgotPasswordRequest();
        forgotRequest.setEmail("reset@example.com");
        authService.forgotPassword(forgotRequest);

        PasswordResetToken resetToken = passwordResetTokenRepository.findAll().stream()
                .filter(token -> token.getUserId().equals(registration.getUserId()))
                .findFirst()
                .orElseThrow();

        authService.login(
                LoginRequest.builder().email("reset@example.com").password("Password123!").build(),
                "127.0.0.1", "JUnit");
        assertThat(refreshTokenRepository.countByUserIdAndRevokedFalse(registration.getUserId())).isGreaterThanOrEqualTo(1);

        ResetPasswordRequest resetRequest = new ResetPasswordRequest();
        resetRequest.setToken(resetToken.getToken());
        resetRequest.setNewPassword("NewPassword456!");
        authService.resetPassword(resetRequest);

        assertThat(refreshTokenRepository.countByUserIdAndRevokedFalse(registration.getUserId())).isZero();
        assertThat(auditLogRepository.countByAction(AuditAction.PASSWORD_RESET.name())).isGreaterThanOrEqualTo(1);
        assertThat(auditLogRepository.countByAction(AuditAction.PASSWORD_RESET_REQUESTED.name())).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldVerifyEmailAndAuditEvent() {
        var registration = registerUser("verify@example.com", "Verify Corp");
        EmailVerificationToken token = emailVerificationTokenRepository.findAll().stream()
                .filter(t -> t.getUserId().equals(registration.getUserId()))
                .findFirst()
                .orElseThrow();

        VerifyEmailRequest verifyRequest = new VerifyEmailRequest();
        verifyRequest.setToken(token.getToken());
        authService.verifyEmail(verifyRequest);

        var user = userRepository.findById(registration.getUserId()).orElseThrow();
        assertThat(user.isEmailVerified()).isTrue();
        assertThat(auditLogRepository.countByAction(AuditAction.EMAIL_VERIFIED.name())).isGreaterThanOrEqualTo(1);
    }

    @Test
    void shouldIsolateTenantsAcrossRegistrationAndUserCreation() {
        var tenantA = registerUser("tenant-a@example.com", "Tenant A Corp");
        var tenantB = registerUser("tenant-b@example.com", "Tenant B Corp");

        assertThat(tenantA.getTenantId()).isNotEqualTo(tenantB.getTenantId());

        CreateUserRequest createRequest = new CreateUserRequest();
        createRequest.setEmail("member@tenant-a.com");
        createRequest.setPassword("Password123!");
        createRequest.setFirstName("Member");
        createRequest.setLastName("A");

        var created = userService.createUser(tenantA.getTenantId(), createRequest);
        assertThat(created.getTenantId()).isEqualTo(tenantA.getTenantId());
        assertThat(created.getTenantId()).isNotEqualTo(tenantB.getTenantId());
    }

    private RegistrationResponse registerUser(String email, String company) {
        RegisterRequest request = new RegisterRequest();
        request.setEmail(email);
        request.setPassword("Password123!");
        request.setFirstName("Test");
        request.setLastName("User");
        request.setCompanyName(company);
        return registrationService.register(request, "127.0.0.1", "IT");
    }

    private void verifyUserEmail(UUID userId) {
        var user = userRepository.findById(userId).orElseThrow();
        user.setEmailVerified(true);
        userRepository.save(user);
    }
}
