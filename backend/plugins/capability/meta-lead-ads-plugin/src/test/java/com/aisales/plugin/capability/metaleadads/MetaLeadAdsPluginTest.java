package com.aisales.plugin.capability.metaleadads;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MetaLeadAdsPluginTest {

    @Test
    void shouldExposeMetadataOnlyCapability() {
        MetaLeadAdsPlugin plugin = new MetaLeadAdsPlugin();
        assertThat(plugin.descriptor().getPluginKey()).isEqualTo(MetaLeadAdsPlugin.PLUGIN_KEY);
        assertThat(plugin.descriptor().getMetadata()).containsEntry("implementsSend", false);
        assertThat(plugin.descriptor().getMetadata()).containsEntry("runtimeOwner", "integration-service");
    }
}
