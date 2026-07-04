package com.aisales.identity.authentication.application;

import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aisales.identity.authentication.domain.entity.PasswordResetToken;
import com.aisales.identity.authentication.infrastructure.configuration.AuthProperties;
import com.aisales.identity.authentication.infrastructure.persistence.PasswordResetTokenRepository;
import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.notification.application.event.PasswordResetRequestedEvent;



@Service
@RequiredArgsConstructor
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthProperties authProperties;
    private final AuditService auditService;

    @Transactional
    public void issueResetToken(UUID tenantId, UUID userId, String email, String firstName) {
        enforceRequestLimits(userId);
        Instant now = Instant.now();
        tokenRepository.invalidatePendingForUser(userId, now);
        String token = UUID.randomUUID().toString();
        tokenRepository.save(PasswordResetToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(now.plusSeconds(authProperties.getPasswordResetExpirationHours() * 3600))
                .createdAt(now)
                .build());
        eventPublisher.publishEvent(new PasswordResetRequestedEvent(tenantId, email, firstName, token));
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
