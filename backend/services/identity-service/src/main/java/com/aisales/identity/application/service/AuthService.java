package com.aisales.identity.application.service;

import com.aisales.common.contracts.auth.AuthResponse;
import com.aisales.common.contracts.auth.LoginRequest;
import com.aisales.common.contracts.auth.RefreshTokenRequest;
import com.aisales.common.contracts.user.CreateUserRequest;
import com.aisales.common.contracts.user.UserDto;
import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.events.model.UserCreatedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.common.exception.exception.UnauthorizedException;
import com.aisales.common.exception.exception.ValidationException;
import com.aisales.identity.api.request.ForgotPasswordRequest;
import com.aisales.identity.api.request.LogoutRequest;
import com.aisales.identity.api.request.ResendVerificationEmailRequest;
import com.aisales.identity.api.request.ResetPasswordRequest;
import com.aisales.identity.api.request.VerifyEmailRequest;
import com.aisales.identity.api.response.MessageResponse;
import com.aisales.identity.api.response.SessionResponse;
import com.aisales.identity.domain.entity.EmailVerificationToken;
import com.aisales.identity.domain.entity.PasswordResetToken;
import com.aisales.identity.domain.entity.RefreshToken;
import com.aisales.identity.domain.entity.User;
import com.aisales.identity.domain.entity.UserSession;
import com.aisales.identity.infrastructure.configuration.AuthProperties;
import com.aisales.identity.infrastructure.persistence.EmailVerificationTokenRepository;
import com.aisales.identity.infrastructure.persistence.PasswordResetTokenRepository;
import com.aisales.identity.infrastructure.persistence.RefreshTokenRepository;
import com.aisales.identity.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
    private final EventPublisher eventPublisher;
    private final PasswordResetTokenService passwordResetTokenService;
    private final EmailVerificationService emailVerificationService;
    private final AuditService auditService;
    private final AuthProperties authProperties;

    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (user.getStatus() != User.UserStatus.ACTIVE) {
            throw new UnauthorizedException("Account is not active");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        if (authProperties.isRequireEmailVerificationForLogin() && !user.isEmailVerified()) {
            throw new UnauthorizedException("Email verification required. Check your inbox or request a new verification email.");
        }
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);
        auditService.log(user.getTenantId(), user.getId(), "USER_LOGIN", "user", user.getId().toString(),
                ipAddress, userAgent, null);
        return tokenService.issueTokens(user, ipAddress, userAgent);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request, String ipAddress, String userAgent) {
        RefreshToken refreshToken = refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (refreshToken.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token expired");
        }
        User user = userRepository.findById(refreshToken.getUserId())
                .orElseThrow(() -> new NotFoundException("User", refreshToken.getUserId()));
        if (authProperties.isRequireEmailVerificationForLogin() && !user.isEmailVerified()) {
            throw new UnauthorizedException("Email verification required");
        }
        refreshTokenService.revokeToken(refreshToken.getToken());
        sessionService.revokeSessionByRefreshTokenId(refreshToken.getId());
        return tokenService.issueTokens(user, ipAddress, userAgent);
    }

    @Transactional
    public MessageResponse logout(LogoutRequest request, UUID userId, String ipAddress, String userAgent) {
        refreshTokenRepository.findByTokenAndRevokedFalse(request.getRefreshToken()).ifPresent(token -> {
            refreshTokenService.revokeToken(token.getToken());
            sessionService.revokeSessionByRefreshTokenId(token.getId());
        });
        auditService.log(null, userId, "USER_LOGOUT", "user", userId.toString(), ipAddress, userAgent, null);
        return MessageResponse.builder().message("Logged out successfully").build();
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
        auditService.log(user.getTenantId(), user.getId(), "EMAIL_VERIFIED", "user", user.getId().toString(),
                null, null, null);
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
                emailVerificationService.issueVerificationToken(
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
        userRepository.save(user);
        token.setConsumedAt(Instant.now());
        passwordResetTokenRepository.save(token);
        sessionService.revokeAllSessions(user.getId());
        refreshTokenRepository.deleteByUserId(user.getId());
        auditService.log(user.getTenantId(), user.getId(), "PASSWORD_RESET", "user", user.getId().toString(),
                null, null, null);
        return MessageResponse.builder().message("Password reset successfully").build();
    }

    @Transactional
    public UserDto createUser(UUID tenantId, CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new ValidationException("Email already registered");
        }
        Set<String> roles = request.getRoles() != null ? request.getRoles() : Set.of("USER");
        User user = User.builder()
                .tenantId(tenantId)
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .status(User.UserStatus.ACTIVE)
                .roles(new HashSet<>(roles))
                .build();
        user = userRepository.save(user);
        eventPublisher.publish(UserCreatedEvent.of(
                tenantId.toString(), user.getId().toString(), user.getEmail(),
                user.getFirstName(), user.getLastName(), user.getRoles(),
                CorrelationIdUtils.getCorrelationId()));
        return toDto(user);
    }

    @Transactional(readOnly = true)
    public UserDto getUser(UUID id) {
        return userRepository.findById(id)
                .map(this::toDto)
                .orElseThrow(() -> new NotFoundException("User", id));
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

    private UserDto toDto(User user) {
        return UserDto.builder()
                .id(user.getId())
                .tenantId(user.getTenantId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .roles(user.getRoles())
                .status(user.getStatus().name())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
