package com.aisales.common.core.util;

/**
 * Shared helpers for logging without leaking PII or secrets.
 * Prefer these over ad-hoc string masking in services.
 */
public final class SensitiveDataRedactor {

    private SensitiveDataRedactor() {
    }

    /** Redacts local-part for structured logs (keeps domain for ops diagnosis). */
    public static String redactEmail(String email) {
        if (email == null || email.isBlank()) {
            return "(none)";
        }
        int at = email.indexOf('@');
        if (at <= 0 || at == email.length() - 1) {
            return "***";
        }
        return "***@" + email.substring(at + 1);
    }

    /** Masks all but last 4 characters of a token-like value. */
    public static String redactToken(String token) {
        if (token == null || token.isBlank()) {
            return "(none)";
        }
        String value = token.trim();
        if (value.length() <= 4) {
            return "****";
        }
        return "****" + value.substring(value.length() - 4);
    }

    /** Masks phone-like strings for logs. */
    public static String redactPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "(none)";
        }
        String digits = phone.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "***";
        }
        return "***" + digits.substring(digits.length() - 4);
    }
}
