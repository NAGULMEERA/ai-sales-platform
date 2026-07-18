package com.aisales.identity.authentication.application;

import com.aisales.common.contracts.auth.AuthResponse;
import com.aisales.common.contracts.auth.LoginRequest;
import com.aisales.common.contracts.auth.RefreshTokenRequest;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.events.model.EmailVerifiedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.UnauthorizedException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.audit.domain.AuditDetails;
import com.aisales.identity.authentication.api.request.ForgotPasswordRequest;
import com.aisales.identity.authentication.api.request.LogoutRequest;
import com.aisales.identity.authentication.api.request.ResendVerificationEmailRequest;
import com.aisales.identity.authentication.api.request.ResetPasswordRequest;
import com.aisales.identity.authentication.api.request.VerifyEmailRequest;
import com.aisales.identity.authentication.api.response.MessageResponse;
import com.aisales.identity.authentication.domain.entity.EmailVerificationToken;
import com.aisales.identity.authentication.domain.entity.PasswordResetToken;
import com.aisales.identity.authentication.domain.entity.RefreshToken;
import com.aisales.identity.authentication.infrastructure.configuration.AuthProperties;
import com.aisales.identity.authentication.infrastructure.persistence.EmailVerificationTokenRepository;
import com.aisales.identity.authentication.infrastructure.persistence.PasswordResetTokenRepository;
import com.aisales.identity.authentication.infrastructure.persistence.RefreshTokenRepository;
import com.aisales.identity.session.api.response.SessionResponse;
import com.aisales.identity.session.application.SessionService;
import com.aisales.identity.user.domain.entity.User;
import com.aisales.identity.user.infrastructure.persistence.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RefreshTokenService refreshTokenService;
    private final SessionService sessionService;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailVerificationService emailVerificationService;
    private final LoginLockoutService loginLockoutService;
    private final AuditService auditService;
    private final EventPublisher eventPublisher;
    private final AuthProperties authProperties;
    private final ObjectProvider<PlatformMetrics> platformMetrics;

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        Optional<User> userOptional = userRepository.findByEmail(request.getEmail());
        if (userOptional.isEmpty()) {
            auditLoginFailure(null, null, "unknown_account", ipAddress, userAgent);
            recordLoginMetric(null, "failure", "unknown_account");
            throw new UnauthorizedException("Invalid credentials");
        }
        User user = userOptional.get();
        loginLockoutService.unlockIfExpired(user);
        if (loginLockoutService.isCurrentlyLocked(user)) {
            auditLoginFailure(user.getTenantId(), user.getId(), "account_locked", ipAddress, userAgent);
            recordLoginMetric(user.getTenantId(), "failure", "account_locked");
            throw new UnauthorizedException("Account is temporarily locked. Try again later.");
        }
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            auditLoginFailure(user.getTenantId(), user.getId(), "account_inactive", ipAddress, userAgent);
            recordLoginMetric(user.getTenantId(), "failure", "account_inactive");
            throw new UnauthorizedException("Account is not active");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            loginLockoutService.recordFailedPasswordAttempt(user.getId(), ipAddress, userAgent);
            auditLoginFailure(user.getTenantId(), user.getId(), "invalid_credentials", ipAddress, userAgent);
            recordLoginMetric(user.getTenantId(), "failure", "invalid_credentials");
            throw new UnauthorizedException("Invalid credentials");
        }
        if (authProperties.isRequireEmailVerificationForLogin() && !user.isEmailVerified()) {
            auditLoginFailure(user.getTenantId(), user.getId(), "email_verification_required", ipAddress, userAgent);
            recordLoginMetric(user.getTenantId(), "failure", "email_verification_required");
            throw new UnauthorizedException("Email verification required. Check your inbox or request a new verification email.");
        }
        loginLockoutService.clearFailedAttempts(user);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        auditService.logSecurityEvent(user.getTenantId(), user.getId(), AuditAction.USER_LOGIN, "user",
                user.getId().toString(), ipAddress, userAgent, null);
        recordLoginMetric(user.getTenantId(), "success", "ok");
        return tokenService.issueTokens(user, ipAddress, userAgent);
    }

    private void recordLoginMetric(UUID tenantId, String outcome, String reason) {
        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        if (metrics == null) {
            return;
        }
        String tenant = tenantId != null ? tenantId.toString() : "unknown";
        metrics.incrementBusinessMetric(MetricNames.AUTH_LOGIN, tenant,
                "outcome", outcome, "reason", reason);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new NotFoundException("User", refreshToken.getUserId()));
        if (loginLockoutService.isCurrentlyLocked(user)) {
            throw new UnauthorizedException("Account is temporarily locked. Try again later.");
        }
        if (authProperties.isRequireEmailVerificationForLogin() && !user.isEmailVerified()) {
            throw new UnauthorizedException("Email verification required");
        }
        return tokenService.rotateTokens(user, request.getRefreshToken(), ipAddress, userAgent);
    }

    @Transactional
    public MessageResponse logout(LogoutRequest request, UUID userId, String ipAddress, String userAgent) {
        refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken()).ifPresent(token -> {
            if (!token.getUserId().equals(userId)) {
                throw new UnauthorizedException("Invalid refresh token");
            }
            refreshTokenService.revokeToken(token.getToken());
        });
        auditService.logSecurityEvent(null, userId, AuditAction.USER_LOGOUT, "user", userId.toString(),
                ipAddress, userAgent, null);
        return MessageResponse.builder().message("Logged out successfully").build();
    }

    @Transactional
    public MessageResponse logoutAllDevices(UUID userId, String ipAddress, String userAgent) {
        refreshTokenService.revokeAllForUser(userId);
        auditService.logSecurityEvent(null, userId, AuditAction.USER_LOGOUT_ALL, "user", userId.toString(),
                ipAddress, userAgent, null);
        return MessageResponse.builder().message("Logged out from all devices").build();
    }

    @Transactional
    public MessageResponse verifyEmail(VerifyEmailRequest request) {
        EmailVerificationToken token = emailVerificationTokenRepository.findByTokenAndConsumedAtIsNull(request.getToken())
                .orElseThrow(() -> new ValidationException("Invalid verification token"));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ValidationException("Verification token expired");
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new NotFoundException("User", token.getUserId()));
        user.setEmailVerified(true);
        userRepository.save(user);
        token.setConsumedAt(Instant.now());
        emailVerificationTokenRepository.save(token);
        auditService.logSecurityEvent(user.getTenantId(), user.getId(), AuditAction.EMAIL_VERIFIED, "user",
                user.getId().toString(), null, null, null);
        eventPublisher.publish(EmailVerifiedEvent.of(
                user.getTenantId().toString(),
                user.getId().toString(),
                user.getEmail(),
                CorrelationIdUtils.get().orElseGet(CorrelationIdUtils::generate)));
        return MessageResponse.builder().message("Email verified successfully").build();
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user ->
                passwordResetTokenService.issueResetToken(
                        user.getTenantId(), user.getId(), user.getEmail(), user.getFirstName()));
        return MessageResponse.builder()
                .message("If the email exists, a reset link has been sent")
                .build();
    }

    @Transactional
    public MessageResponse resendVerificationEmail(ResendVerificationEmailRequest request) {
        userRepository.findByEmail(request.getEmail()).ifPresent(user -> {
            if (!user.isEmailVerified()) {
                emailVerificationService.resendVerificationToken(
                        user.getTenantId(), user.getId(), user.getEmail(), user.getFirstName());
            }
        });
        return MessageResponse.builder()
                .message("If the email exists and is not verified, a verification email has been sent")
                .build();
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken token = passwordResetTokenRepository.findByTokenAndConsumedAtIsNull(request.getToken())
                .orElseThrow(() -> new ValidationException("Invalid reset token"));
        if (token.getExpiresAt().isBefore(Instant.now())) {
            throw new ValidationException("Reset token expired");
        }
        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new NotFoundException("User", token.getUserId()));
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        loginLockoutService.clearFailedAttempts(user);
        userRepository.save(user);
        token.setConsumedAt(Instant.now());
        passwordResetTokenRepository.save(token);
        passwordResetTokenRepository.invalidatePendingForUser(user.getId(), Instant.now());
        refreshTokenService.revokeAllForUser(user.getId());
        auditService.logSecurityEvent(user.getTenantId(), user.getId(), AuditAction.PASSWORD_RESET, "user",
                user.getId().toString(), null, null, null);
        return MessageResponse.builder().message("Password reset successfully").build();
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> listSessions(UUID userId, UUID currentRefreshTokenId) {
        return sessionService.listActiveSessions(userId).stream()
                .map(session -> SessionResponse.builder()
                        .sessionId(session.getId())
                        .deviceLabel(session.getDeviceLabel())
                        .ipAddress(session.getIpAddress())
                        .createdAt(session.getCreatedAt())
                        .expiresAt(session.getExpiresAt())
                        .current(currentRefreshTokenId != null
                                && currentRefreshTokenId.equals(session.getRefreshTokenId()))
                        .build())
                .toList();
    }

    private void auditLoginFailure(UUID tenantId, UUID userId, String reason, String ipAddress, String userAgent) {
        auditService.logSecurityEventIndependent(tenantId, userId, AuditAction.LOGIN_FAILED, "user",
                userId != null ? userId.toString() : null, ipAddress, userAgent, AuditDetails.reason(reason));
    }
}
