package com.aisales.identity.infrastructure.client;

import com.aisales.common.security.util.JwtTokenProvider;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Mints a short-lived platform JWT that identity-service presents to notification-service when
 * sending transactional emails directly (bypassing the gateway) via {@link NotificationHttpSender}.
 *
 * <p>Reuses the platform's existing shared JWT infrastructure (Rule 06 Part 1: "Internal
 * communication must use... Authenticated identities, JWT propagation") rather than introducing a
 * separate API-key mechanism. The token carries the {@code SERVICE} role only - no end-user
 * identity - and notification-service's security config requires exactly that role for its
 * notification-sending endpoints, so a leaked end-user access token cannot be replayed here.
 *
 * <p>The tenant claim uses the nil UUID as a "no real tenant" sentinel: {@link
 * com.aisales.common.core.util.TenantContext#getTenantIdAsUuid()} calls {@code UUID.fromString}
 * unconditionally once a tenant id is present, so a non-UUID placeholder like {@code "system"}
 * would throw an uncaught {@link IllegalArgumentException} inside the downstream service's JWT
 * filter.
 */
@Component
public class InternalServiceTokenProvider {

    private static final String SERVICE_SUBJECT = "identity-service";
    private static final String SYSTEM_TENANT_ID = "00000000-0000-0000-0000-000000000000";
    private static final Set<String> SERVICE_ROLES = Set.of("SERVICE");

    private final JwtTokenProvider jwtTokenProvider;

    public InternalServiceTokenProvider(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public String mintServiceToken() {
        return jwtTokenProvider
                .generateAccessToken(SERVICE_SUBJECT, SYSTEM_TENANT_ID, null, null, SERVICE_ROLES, Set.of())
                .getAccessToken();
    }
}
