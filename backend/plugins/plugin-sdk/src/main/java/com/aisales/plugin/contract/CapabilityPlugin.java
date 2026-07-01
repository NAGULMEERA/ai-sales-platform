package com.aisales.plugin.contract;

import java.util.UUID;

/**
 * Contract for reusable capability plugins (WhatsApp, Voice, Payment, etc.).
 */
public interface CapabilityPlugin {

    String getPluginId();

    String getCapabilityType();

    String getVersion();

    boolean isHealthy(UUID tenantId);
}
