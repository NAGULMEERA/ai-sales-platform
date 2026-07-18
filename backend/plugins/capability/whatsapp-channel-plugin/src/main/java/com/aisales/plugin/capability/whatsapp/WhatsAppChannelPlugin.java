package com.aisales.plugin.capability.whatsapp;

import com.aisales.plugin.contract.CapabilityPlugin;
import com.aisales.plugin.contract.PluginDescriptor;
import com.aisales.plugin.contract.PluginType;
import java.util.List;
import java.util.Map;

/**
 * Capability stub: describes WhatsApp notification configuration.
 * Does not call Meta APIs — runtime remains outside this plugin jar.
 */
public class WhatsAppChannelPlugin implements CapabilityPlugin {

    public static final String PLUGIN_KEY = "whatsapp-channel";

    public static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder()
            .pluginKey(PLUGIN_KEY)
            .type(PluginType.CAPABILITY)
            .version("1.0.0")
            .displayName("WhatsApp Channel")
            .description("Capability metadata for WhatsApp notifications. Meta Cloud API remains outside Platform Core plugins.")
            .capabilities(List.of("notification.whatsapp"))
            .configSchemaJson("""
                    {"type":"object","properties":{"phoneNumberId":{"type":"string"},"wabaId":{"type":"string"}},"required":["phoneNumberId"]}
                    """)
            .defaultConfig(Map.of("phoneNumberId", "", "wabaId", ""))
            .metadata(Map.of("runtimeOwner", "notification-service", "implementsSend", false))
            .build();

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }
}
