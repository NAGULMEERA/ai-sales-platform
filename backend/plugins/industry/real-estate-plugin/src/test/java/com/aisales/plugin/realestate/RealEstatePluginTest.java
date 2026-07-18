package com.aisales.plugin.realestate;

import static org.assertj.core.api.Assertions.assertThat;

import com.aisales.plugin.contract.PluginType;
import org.junit.jupiter.api.Test;

class RealEstatePluginTest {

    @Test
    void shouldExposeIndustryMetadataWithoutMicroservice() {
        var plugin = new RealEstatePlugin();
        assertThat(plugin.descriptor().getPluginKey()).isEqualTo("real-estate");
        assertThat(plugin.descriptor().getType()).isEqualTo(PluginType.INDUSTRY);
        assertThat(plugin.getIndustryCode()).isEqualTo("REAL_ESTATE");
        assertThat(plugin.descriptor().getMetadata()).containsEntry("ownsMicroservice", false);
        assertThat(plugin.descriptor().getDefaultConfig()).containsKey("catalogAttributeKeys");
    }
}
