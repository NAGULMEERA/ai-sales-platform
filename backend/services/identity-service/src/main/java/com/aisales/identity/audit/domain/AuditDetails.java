package com.aisales.identity.audit.domain;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * Safe audit detail payloads. Must never contain passwords, tokens, or secrets.
 */
public final class AuditDetails {

    private AuditDetails() {
    }

    public static String reason(String reason) {
        return "{\"reason\":\"" + escape(reason) + "\"}";
    }

    public static String plan(String plan) {
        return "{\"plan\":\"" + escape(plan) + "\"}";
    }

    public static String roles(Set<String> roles) {
        if (roles == null || roles.isEmpty()) {
            return "{\"roles\":[]}";
        }
        String joined = roles.stream()
                .map(AuditDetails::escape)
                .map(role -> "\"" + role + "\"")
                .collect(Collectors.joining(","));
        return "{\"roles\":[" + joined + "]}";
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
