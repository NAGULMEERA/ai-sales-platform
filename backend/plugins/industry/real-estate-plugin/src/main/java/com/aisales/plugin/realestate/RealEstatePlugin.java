package com.aisales.plugin.realestate;

import com.aisales.plugin.contract.IndustryPlugin;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class RealEstatePlugin implements IndustryPlugin {

    public static final String PLUGIN_ID = "real-estate";

    @Override
    public String getPluginId() {
        return PLUGIN_ID;
    }

    @Override
    public String getIndustryCode() {
        return "REAL_ESTATE";
    }

    @Override
    public String getVersion() {
        return "1.0.0";
    }

    @Override
    public void onEnable(UUID tenantId) {
        // Sprint 5: register industry workflows and catalog extensions
    }

    @Override
    public void onDisable(UUID tenantId) {
        // Sprint 5: cleanup tenant-specific plugin state
    }
}
