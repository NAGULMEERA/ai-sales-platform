package com.aisales.plugin.capability.email;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.plugin.contract.PluginType;
import org.junit.jupiter.api.Test;

class EmailChannelPluginTest {

    @Test
    void shouldExposeMetadataOnlyDescriptor() {
        var plugin = new EmailChannelPlugin();
        assertThat(plugin.descriptor().getPluginKey()).isEqualTo("email-channel");
        assertThat(plugin.descriptor().getType()).isEqualTo(PluginType.CAPABILITY);
        assertThat(plugin.getCapabilityType()).isEqualTo("notification.email");
        assertThat(plugin.descriptor().getMetadata()).containsEntry("implementsSend", false);
    }
}
