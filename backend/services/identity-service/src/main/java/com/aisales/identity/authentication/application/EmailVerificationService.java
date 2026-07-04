package com.aisales.identity.authentication.application;

import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.aisales.identity.authentication.domain.entity.EmailVerificationToken;
import com.aisales.identity.authentication.infrastructure.configuration.AuthProperties;
import com.aisales.identity.authentication.infrastructure.persistence.EmailVerificationTokenRepository;
import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.audit.domain.AuditAction;
import com.aisales.identity.notification.application.event.EmailVerificationRequestedEvent;



@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokenRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AuthProperties authProperties;
    private final AuditService auditService;

    /**
     * Issues the initial verification token during registration. Replaces any pending token
     * so only one active link exists; no resend rate limits apply.
     */
    @Transactional
    public void issueVerificationToken(UUID tenantId, UUID userId, String email, String firstName) {
        createActiveToken(tenantId, userId, email, firstName);
    }

    /**
     * Resend path with cooldown, rolling window cap, and single active token enforcement.
     */
    @Transactional
    public void resendVerificationToken(UUID tenantId, UUID userId, String email, String firstName) {
        enforceResendLimits(userId);
        createActiveToken(tenantId, userId, email, firstName);
    }

    private void createActiveToken(UUID tenantId, UUID userId, String email, String firstName) {
        Instant now = Instant.now();
        tokenRepository.invalidatePendingForUser(userId, now);
        String token = UUID.randomUUID().toString();
        tokenRepository.save(EmailVerificationToken.builder()
                .userId(userId)
                .token(token)
                .expiresAt(now.plusSeconds(authProperties.getEmailVerificationExpirationHours() * 3600))
                .createdAt(now)
                .build());
        eventPublisher.publishEvent(new EmailVerificationRequestedEvent(tenantId, email, firstName, token));
        auditService.logSecurityEvent(tenantId, userId, AuditAction.EMAIL_VERIFICATION_SENT, "user",
                userId.toString(), null, null, null);
    }

    private void enforceResendLimits(UUID userId) {
        Instant now = Instant.now();
        Instant windowStart = now.minusSeconds(authProperties.getVerificationResendWindowHours() * 3600L);
        long issuedInWindow = tokenRepository.countByUserIdAndCreatedAtAfter(userId, windowStart);
        if (issuedInWindow >= authProperties.getMaxVerificationResendsPerWindow()) {
            throw new BusinessException(
                    ErrorCode.AUTH_RATE_LIMIT,
                    "Too many verification emails requested. Try again later.");
        }
        tokenRepository.findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(userId).ifPresent(latest -> {
            Instant cooldownEnds = latest.getCreatedAt()
                    .plusSeconds(authProperties.getVerificationResendCooldownMinutes() * 60L);
            if (now.isBefore(cooldownEnds)) {
                throw new BusinessException(
                        ErrorCode.AUTH_RATE_LIMIT,
                        "Please wait before requesting another verification email.");
            }
        });
    }
}
