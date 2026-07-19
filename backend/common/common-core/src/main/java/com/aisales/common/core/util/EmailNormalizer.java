package com.aisales.common.core.util;

import java.util.Locale;
import org.springframework.util.StringUtils;

/** Canonicalizes emails for storage and lookup (Locale.ROOT lower-case trim). */
public final class EmailNormalizer {

    private EmailNormalizer() {}

    public static String normalize(String email) {
        if (!StringUtils.hasText(email)) {
            return email;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
