package com.aisales.plugin.contract;

import java.util.UUID;

/**
 * Contract for industry-specific plugins (Real Estate, Education, Healthcare, etc.).
 */
public interface IndustryPlugin {

    String getPluginId();

    String getIndustryCode();

    String getVersion();

    void onEnable(UUID tenantId);

    void onDisable(UUID tenantId);
}
