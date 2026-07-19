package com.aisales.ai.domain.prompt;

import java.util.UUID;

/**
 * Well-known platform prompt seed tenant. Rows under this tenant are fallbacks when a
 * requesting tenant has no override for the same prompt code. Not an identity-service tenant.
 */
public final class PlatformPromptConstants {

    public static final UUID PLATFORM_TENANT_ID =
            UUID.fromString("00000000-0000-4000-8000-0000000000aa");

    private PlatformPromptConstants() {
    }
}
