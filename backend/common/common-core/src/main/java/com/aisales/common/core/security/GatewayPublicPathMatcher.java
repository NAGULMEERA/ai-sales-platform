package com.aisales.common.core.security;

import com.aisales.common.core.constant.SecurityConstants;
import org.springframework.util.AntPathMatcher;

/**
 * Matches request paths against platform public-route patterns (shared by gateway and servlet services).
 */
public final class GatewayPublicPathMatcher {

    private static final AntPathMatcher MATCHER = new AntPathMatcher();

    private GatewayPublicPathMatcher() {
    }

    public static boolean isPublicPath(String path) {
        if (path == null || path.isBlank()) {
            return false;
        }
        for (String pattern : SecurityConstants.PUBLIC_PATHS) {
            if (MATCHER.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }
}
