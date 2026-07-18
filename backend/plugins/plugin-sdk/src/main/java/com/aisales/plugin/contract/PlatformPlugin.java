package com.aisales.plugin.contract;

import java.util.UUID;

/**
 * Base contract for all plugins. Implementations must expose a {@link PluginDescriptor}
 * and must not contain business rules or call other microservices' internals.
 */
public interface PlatformPlugin {

    PluginDescriptor descriptor();

    default String getPluginId() {
        return descriptor().getPluginKey();
    }

    default String getVersion() {
        return descriptor().getVersion();
    }

    default boolean isHealthy(UUID tenantId) {
        return true;
    }
}
