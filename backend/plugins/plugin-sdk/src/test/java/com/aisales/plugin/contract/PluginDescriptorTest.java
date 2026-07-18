package com.aisales.plugin.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;

class PluginDescriptorTest {

    @Test
    void shouldRequireIndustryCodeForIndustryPlugins() {
        assertThatThrownBy(() -> PluginDescriptor.builder()
                        .pluginKey("x")
                        .type(PluginType.INDUSTRY)
                        .version("1.0.0")
                        .displayName("X")
                        .capabilities(List.of("industry.x"))
                        .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("industryCode");
    }

    @Test
    void shouldBuildCapabilityDescriptor() {
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .pluginKey("email-channel")
                .type(PluginType.CAPABILITY)
                .version("1.0.0")
                .displayName("Email")
                .capabilities(List.of("notification.email"))
                .build();
        assertThat(descriptor.getType()).isEqualTo(PluginType.CAPABILITY);
        assertThat(descriptor.getCapabilities()).containsExactly("notification.email");
        assertThat(descriptor.getMinPlatformVersion()).isEqualTo("1.0.0");
    }

    @Test
    void shouldHonorExplicitMinPlatformVersion() {
        PluginDescriptor descriptor = PluginDescriptor.builder()
                .pluginKey("future")
                .type(PluginType.CAPABILITY)
                .version("2.0.0")
                .minPlatformVersion("1.5.0")
                .displayName("Future")
                .capabilities(List.of("x"))
                .build();
        assertThat(descriptor.getMinPlatformVersion()).isEqualTo("1.5.0");
    }
}
