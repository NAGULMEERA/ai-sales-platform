package com.aisales.plugin.contract;

/**
 * Plugin classification. Industry plugins contribute config/metadata only;
 * capability plugins describe reusable integrations (not business methods).
 */
public enum PluginType {
    CAPABILITY,
    INDUSTRY
}
