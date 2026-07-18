package com.aisales.plugin.capability.metaleadads;

import com.aisales.plugin.contract.CapabilityPlugin;
import com.aisales.plugin.contract.PluginDescriptor;
import com.aisales.plugin.contract.PluginType;
import java.util.List;
import java.util.Map;

/**
 * Capability stub: Facebook / Instagram Lead Ads configuration.
 * Webhook runtime and Graph API remain in integration-service.
 */
public class MetaLeadAdsPlugin implements CapabilityPlugin {

    public static final String PLUGIN_KEY = "meta-lead-ads";

    public static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder()
            .pluginKey(PLUGIN_KEY)
            .type(PluginType.CAPABILITY)
            .version("1.0.0")
            .minPlatformVersion("1.0.0")
            .displayName("Meta Lead Ads")
            .description(
                    "Capability metadata for Facebook/Instagram Lead Ads ingest. "
                            + "Webhook + instant voice qualify runtime remains in integration-service.")
            .capabilities(List.of("lead.ingest.meta", "lead.instant_voice_qualify"))
            .configSchemaJson("""
                    {"type":"object","properties":{"pageId":{"type":"string"},"promptCode":{"type":"string"}},"required":["pageId"]}
                    """)
            .defaultConfig(Map.of("pageId", "", "promptCode", "LEAD_QUALIFY_REAL_ESTATE"))
            .metadata(Map.of(
                    "runtimeOwner", "integration-service",
                    "implementsSend", false,
                    "webhookPath", "/api/v1/integrations/webhooks/meta/leadgen"))
            .build();

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }
}
