package com.aisales.identity.authentication.application;

import com.aisales.common.core.util.CorrelationIdUtils;
import com.aisales.common.events.model.PasswordResetRequestedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.authentication.domain.entity.PasswordResetToken;
import com.aisales.identity.authentication.infrastructure.configuration.AuthProperties;
import com.aisales.identity.authentication.infrastructure.persistence.PasswordResetTokenRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository tokenRepository;
    private final EventPublisher eventPublisher;
    private final AuthProperties authProperties;
    private final AuditService auditService;

    @Transactional
    public void issueResetToken(UUID tenantId, UUID userId, String email, String firstName) {
        enforceRequestLimits(userId);
        Instant now = Instant.now();
        tokenRepository.invalidatePendingForUser(userId, now);
        String rawToken = UUID.randomUUID().toString();
        tokenRepository.save(PasswordResetToken.builder()
                .userId(userId)
                .token(RefreshTokenHasher.sha256Hex(rawToken))
                .expiresAt(now.plusSeconds(authProperties.getPasswordResetExpirationHours() * 3600))
                .createdAt(now)
                .build());

        String resetLink = authProperties.getPasswordResetLinkBaseUrl() + "?token=" + rawToken;
        String correlationId = CorrelationIdUtils.get()
                .orElseGet(CorrelationIdUtils::generate);
        // Raw token is required for the email link only; DB stores SHA-256 digest.
        eventPublisher.publish(PasswordResetRequestedEvent.of(
                tenantId != null ? tenantId.toString() : null,
                userId.toString(),
                email,
                firstName,
                rawToken,
                resetLink,
                correlationId));

        auditService.logSecurityEvent(tenantId, userId, AuditAction.PASSWORD_RESET_REQUESTED, "user",
                userId.toString(), null, null, null);
    }

    private void enforceRequestLimits(UUID userId) {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(authProperties.getPasswordResetRequestWindowHours() * 3600L);
        long issuedInWindow = tokenRepository.countByUserIdAndCreatedAtAfter(userId, windowStart);
        if (issuedInWindow >= authProperties.getMaxPasswordResetRequestsPerWindow()) {
            throw new BusinessException(
                    ErrorCode.AUTH_RATE_LIMIT,
                    "Too many password reset requests. Try again later.");
        }
        tokenRepository.findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(userId).ifPresent(latest -> {
            Instant cooldownEnds = latest.getCreatedAt()
                    .plusSeconds(authProperties.getPasswordResetCooldownMinutes() * 60L);
            if (now.isBefore(cooldownEnds)) {
                throw new BusinessException(
                        ErrorCode.AUTH_RATE_LIMIT,
                        "Please wait before requesting another password reset email.");
            }
        });
    }
}
