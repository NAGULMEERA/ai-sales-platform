package com.aisales.identity.authentication.infrastructure.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;


@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "aisales.auth")
public class AuthProperties {

    /**
     * When true, registration returns JWT tokens immediately (auto-login).
     * Set to false when email verification must complete before first login.
     */
    private boolean autoLoginAfterRegister = false;

    /**
     * When true, login is rejected until users.email_verified is true.
     */
    private boolean requireEmailVerificationForLogin = true;

    private long emailVerificationExpirationHours = 24;
    private long passwordResetExpirationHours = 1;

    /** Minimum minutes between verification email resend requests for the same user. */
    private int verificationResendCooldownMinutes = 5;

    /** Maximum verification emails issued per user within the resend window. */
    private int maxVerificationResendsPerWindow = 5;

    /** Rolling window (hours) for verification resend rate limiting. */
    private int verificationResendWindowHours = 24;

    /** Maximum password reset emails per user within the reset request window. */
    private int maxPasswordResetRequestsPerWindow = 5;

    /** Rolling window (hours) for password reset rate limiting. */
    private int passwordResetRequestWindowHours = 24;

    /** Minimum minutes between password reset requests for the same user. */
    private int passwordResetCooldownMinutes = 5;

    /** Frontend or dev link base used in verification emails. */
    private String verificationLinkBaseUrl = "http://localhost:3000/verify-email";

    /** Frontend or dev link base used in password reset emails. */
    private String passwordResetLinkBaseUrl = "http://localhost:3000/reset-password";

    /** Failed password attempts before the account is locked. */
    private int maxFailedLoginAttempts = 5;

    /** How long a locked account stays locked before automatic unlock on next login. */
    private int lockoutDurationMinutes = 15;
}
