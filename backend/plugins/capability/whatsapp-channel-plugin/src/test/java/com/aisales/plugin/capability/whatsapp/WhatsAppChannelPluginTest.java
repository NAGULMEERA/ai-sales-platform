package com.aisales.plugin.capability.whatsapp;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.plugin.contract.PluginType;
import org.junit.jupiter.api.Test;

class WhatsAppChannelPluginTest {

    @Test
    void shouldExposeMetadataOnlyDescriptor() {
        var plugin = new WhatsAppChannelPlugin();
        assertThat(plugin.descriptor().getPluginKey()).isEqualTo("whatsapp-channel");
        assertThat(plugin.descriptor().getType()).isEqualTo(PluginType.CAPABILITY);
        assertThat(plugin.getCapabilityType()).isEqualTo("notification.whatsapp");
        assertThat(plugin.descriptor().getMetadata()).containsEntry("implementsSend", false);
    }
}
