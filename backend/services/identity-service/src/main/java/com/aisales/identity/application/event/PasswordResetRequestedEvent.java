package com.aisales.identity.application.event;

import java.util.UUID;

/**
 * Raised when a user requests a password reset email. Delivered only after
 * the enclosing transaction commits (see {@code EmailEventListener}).
 */
public record PasswordResetRequestedEvent(
        UUID tenantId,
        String email,
        String firstName,
        String token) {
}
