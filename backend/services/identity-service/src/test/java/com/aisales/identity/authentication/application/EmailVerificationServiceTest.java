package com.aisales.identity.authentication.application;

import com.aisales.common.exception.exception.BusinessException;
import com.aisales.common.exception.model.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import com.aisales.identity.audit.application.AuditService;
import com.aisales.identity.authentication.domain.entity.EmailVerificationToken;
import com.aisales.identity.authentication.infrastructure.configuration.AuthProperties;
import com.aisales.identity.authentication.infrastructure.persistence.EmailVerificationTokenRepository;



import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

    @Mock private EmailVerificationTokenRepository tokenRepository;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private AuditService auditService;

    private AuthProperties authProperties;
    private EmailVerificationService emailVerificationService;

    @BeforeEach
    void setUp() {
        authProperties = new AuthProperties();
        authProperties.setVerificationResendCooldownMinutes(5);
        authProperties.setMaxVerificationResendsPerWindow(3);
        authProperties.setVerificationResendWindowHours(24);
        emailVerificationService = new EmailVerificationService(tokenRepository, eventPublisher, authProperties, auditService);
    }

    @Test
    void shouldInvalidatePendingTokensOnInitialIssue() {
        UUID userId = UUID.randomUUID();
        when(tokenRepository.save(any(EmailVerificationToken.class))).thenAnswer(inv -> inv.getArgument(0));

        emailVerificationService.issueVerificationToken(UUID.randomUUID(), userId, "a@b.com", "A");

        verify(tokenRepository).invalidatePendingForUser(eq(userId), any(Instant.class));
        verify(tokenRepository).save(any(EmailVerificationToken.class));
    }

    @Test
    void shouldRejectResendWithinCooldown() {
        UUID userId = UUID.randomUUID();
        when(tokenRepository.countByUserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(1L);
        when(tokenRepository.findTopByUserIdAndConsumedAtIsNullOrderByCreatedAtDesc(userId))
                .thenReturn(Optional.of(EmailVerificationToken.builder()
                        .createdAt(Instant.now())
                        .build()));

        assertThatThrownBy(() -> emailVerificationService.resendVerificationToken(
                UUID.randomUUID(), userId, "a@b.com", "A"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_RATE_LIMIT);
    }

    @Test
    void shouldRejectResendWhenWindowCapReached() {
        UUID userId = UUID.randomUUID();
        when(tokenRepository.countByUserIdAndCreatedAtAfter(eq(userId), any())).thenReturn(3L);

        assertThatThrownBy(() -> emailVerificationService.resendVerificationToken(
                UUID.randomUUID(), userId, "a@b.com", "A"))
                .isInstanceOf(BusinessException.class)
                .extracting(ex -> ((BusinessException) ex).getErrorCode())
                .isEqualTo(ErrorCode.AUTH_RATE_LIMIT);
    }
}
