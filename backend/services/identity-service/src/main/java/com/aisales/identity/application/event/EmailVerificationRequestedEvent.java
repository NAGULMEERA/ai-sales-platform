package com.aisales.identity.application.event;

import java.util.UUID;

/**
 * Raised when a user needs a verification email. Published inside the owning
 * transaction and delivered by a listener that only runs after commit, so a
 * rolled-back registration or resend never results in an email being sent for
 * data that does not exist.
 */
public record EmailVerificationRequestedEvent(
        UUID tenantId,
        String email,
        String firstName,
        String token) {
}
