package com.aisales.plugin.contract;

import java.util.UUID;

/**
 * Industry plugin contributes metadata/config (attribute schemas, suggested pipeline keys).
 * Must not implement industry business methods (e.g. qualifyLead) or own industry microservices.
 */
public interface IndustryPlugin extends PlatformPlugin {

    default String getIndustryCode() {
        return descriptor().getIndustryCode();
    }

    /**
     * Metadata lifecycle hook only. Do not register business workflows or mutate aggregates here.
     */
    default void onEnable(UUID tenantId) {
        // no-op — enablement is persisted by marketplace-service
    }

    /**
     * Metadata lifecycle hook only.
     */
    default void onDisable(UUID tenantId) {
        // no-op — disablement is persisted by marketplace-service
    }
}
