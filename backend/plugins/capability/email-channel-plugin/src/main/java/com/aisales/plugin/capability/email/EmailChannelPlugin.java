package com.aisales.plugin.capability.email;

import com.aisales.plugin.contract.CapabilityPlugin;
import com.aisales.plugin.contract.PluginDescriptor;
import com.aisales.plugin.contract.PluginType;
import java.util.List;
import java.util.Map;

/**
 * Capability stub: describes email notification configuration.
 * Does not send email — runtime remains in notification-service.
 */
public class EmailChannelPlugin implements CapabilityPlugin {

    public static final String PLUGIN_KEY = "email-channel";

    public static final PluginDescriptor DESCRIPTOR = PluginDescriptor.builder()
            .pluginKey(PLUGIN_KEY)
            .type(PluginType.CAPABILITY)
            .version("1.0.0")
            .displayName("Email Channel")
            .description("Capability metadata for email notification delivery. SMTP remains in notification-service.")
            .capabilities(List.of("notification.email"))
            .configSchemaJson("""
                    {"type":"object","properties":{"fromAddress":{"type":"string"},"replyTo":{"type":"string"}},"required":["fromAddress"]}
                    """)
            .defaultConfig(Map.of("fromAddress", "", "replyTo", ""))
            .metadata(Map.of("runtimeOwner", "notification-service", "implementsSend", false))
            .build();

    @Override
    public PluginDescriptor descriptor() {
        return DESCRIPTOR;
    }
}
