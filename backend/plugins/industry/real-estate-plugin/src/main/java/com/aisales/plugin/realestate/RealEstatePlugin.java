package com.aisales.plugin.realestate;

import com.aisales.plugin.contract.IndustryPlugin;
import com.aisales.plugin.contract.PluginDescriptor;
import com.aisales.plugin.contract.PluginType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Industry metadata plugin for Real Estate.
 * Contributes suggested catalog attribute keys and pipeline template references.
 * Does not own an industry microservice or Lead subtypes.
 */
public class RealEstatePlugin implements IndustryPlugin {

    public static final String PLUGIN_KEY = "real-estate";

    public static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder()
            .pluginKey(PLUGIN_KEY)
            .type(PluginType.INDUSTRY)
            .version("1.0.0")
            .displayName("Real Estate Industry")
            .description("Industry metadata: suggested catalog attribute keys and pipeline template references. No industry microservice.")
            .capabilities(List.of("industry.real_estate", "catalog.attributes", "pipeline.template"))
            .industryCode("REAL_ESTATE")
            .configSchemaJson("""
                    {"type":"object","properties":{"defaultPipelineCode":{"type":"string"},"catalogAttributeKeys":{"type":"array","items":{"type":"string"}}}}
                    """)
            .defaultConfig(Map.of(
                    "defaultPipelineCode", "DEFAULT_SALES_V1",
                    "catalogAttributeKeys", List.of("bedrooms", "bathrooms", "location", "price")))
            .metadata(Map.of("ownsMicroservice", false, "leadSubtype", false))
            .build();

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }

    @Override
    public void onEnable(UUID tenantId) {
        // Enablement is persisted by marketplace-service. No business registration here.
    }

    @Override
    public void onDisable(UUID tenantId) {
        // Disablement is persisted by marketplace-service.
    }
}
