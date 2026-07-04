package com.aisales.identity.notification.application.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import com.aisales.identity.notification.application.EmailNotificationService;


/**
 * Delivers transactional emails strictly after the originating database
 * transaction has committed. This keeps the outbound HTTP call to
 * notification-service out of the business transaction so:
 * - a rollback (e.g. a later validation failure) never sends an email for
 *   data that was never persisted;
 * - the database connection/transaction is not held open for the duration
 *   of a network call.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailEventListener {

    private final EmailNotificationService emailNotificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onEmailVerificationRequested(EmailVerificationRequestedEvent event) {
        try {
            emailNotificationService.sendVerificationEmail(
                    event.tenantId(), event.email(), event.firstName(), event.token());
        } catch (Exception ex) {
            log.warn("Failed to deliver verification email to {}: {}", event.email(), ex.getMessage());
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        try {
            emailNotificationService.sendPasswordResetEmail(
                    event.tenantId(), event.email(), event.firstName(), event.token());
        } catch (Exception ex) {
            log.warn("Failed to deliver password reset email to {}: {}", event.email(), ex.getMessage());
        }
    }
}
