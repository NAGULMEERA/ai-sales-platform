package com.aisales.identity.audit.domain;

/**
 * Standard security and lifecycle audit actions. Never log passwords, tokens, or secrets in details.
 */
public enum AuditAction {

    USER_LOGIN,
    LOGIN_FAILED,
    ACCOUNT_LOCKED,
    USER_LOGOUT,
    USER_LOGOUT_ALL,
    USER_REGISTERED,
    USER_CREATED,
    EMAIL_VERIFIED,
    EMAIL_VERIFICATION_SENT,
    PASSWORD_RESET,
    PASSWORD_RESET_REQUESTED,
    PASSWORD_CHANGED,
    OAUTH_LOGIN,
    OAUTH_LOGIN_FAILED,
    SUBSCRIPTION_CREATED,
    SUBSCRIPTION_UPGRADED,
    ROLE_ASSIGNED,
    PERMISSION_CACHE_EVICTED
}
