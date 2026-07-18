package com.aisales.plugin.contract;

/**
 * Reusable capability plugin (notification channel metadata, calendar, payment, etc.).
 * Describes integration configuration — does not send messages or own business state.
 */
public interface CapabilityPlugin extends PlatformPlugin {

    default String getCapabilityType() {
        var capabilities = descriptor().getCapabilities();
        return capabilities.isEmpty() ? "UNKNOWN" : capabilities.get(0);
    }
}
