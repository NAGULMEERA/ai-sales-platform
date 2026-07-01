package com.aisales.identity.infrastructure.configuration;

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

    /** Frontend or dev link base used in verification emails. */
    private String verificationLinkBaseUrl = "http://localhost:3000/verify-email";

    /** Frontend or dev link base used in password reset emails. */
    private String passwordResetLinkBaseUrl = "http://localhost:3000/reset-password";
}
