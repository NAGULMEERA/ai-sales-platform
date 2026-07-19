package com.aisales.common.core.util;

/**
 * Small string helpers for messaging and DTO assembly. Prefer over ad-hoc null checks.
 */
public final class ObjectStrings {

    private ObjectStrings() {
    }

    /** Returns empty string when {@code value} is null; otherwise {@link String#valueOf(Object)}. */
    public static String nullSafe(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
