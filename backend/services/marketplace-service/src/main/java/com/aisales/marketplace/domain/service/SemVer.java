package com.aisales.marketplace.domain.service;

import com.aisales.common.exception.exception.ValidationException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Minimal major.minor.patch compare for plugin ↔ platform compatibility.
 * Pre-release / build metadata suffixes are ignored after the numeric core.
 */
public final class SemVer {

    private static final Pattern CORE = Pattern.compile("^(\\d+)(?:\\.(\\d+))?(?:\\.(\\d+))?");

    private SemVer() {
    }

    /**
     * @return negative if left &lt; right, zero if equal, positive if left &gt; right
     */
    public static int compare(String left, String right) {
        int[] a = parse(left);
        int[] b = parse(right);
        for (int i = 0; i < 3; i++) {
            int diff = Integer.compare(a[i], b[i]);
            if (diff != 0) {
                return diff;
            }
        }
        return 0;
    }

    public static boolean isAtLeast(String candidate, String minimum) {
        return compare(candidate, minimum) >= 0;
    }

    private static int[] parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("Version is required");
        }
        Matcher matcher = CORE.matcher(raw.trim());
        if (!matcher.find()) {
            throw new ValidationException("Invalid semver: " + raw);
        }
        return new int[] {
                Integer.parseInt(matcher.group(1)),
                matcher.group(2) != null ? Integer.parseInt(matcher.group(2)) : 0,
                matcher.group(3) != null ? Integer.parseInt(matcher.group(3)) : 0
        };
    }
}
