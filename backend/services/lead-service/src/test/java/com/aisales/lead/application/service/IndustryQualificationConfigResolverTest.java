package com.aisales.lead.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.client.MarketplaceServiceClient;
import com.aisales.common.contracts.lead.QualifyLeadWithAiRequest;
import com.aisales.common.contracts.plugin.PluginInstallationDto;
import com.aisales.common.contracts.plugin.PluginInstallationStatus;
import com.aisales.common.contracts.plugin.PluginTypeDto;
import com.aisales.common.core.dto.ApiResponse;
import com.aisales.common.exception.exception.ValidationException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IndustryQualificationConfigResolverTest {

    @Mock private MarketplaceServiceClient marketplaceServiceClient;

    private IndustryQualificationConfigResolver resolver;

    @BeforeEach
    void setUp() {
        resolver = new IndustryQualificationConfigResolver(marketplaceServiceClient);
    }

    @Test
    void shouldPreferCallerOverride() {
        IndustryQualificationConfigResolver.Resolved resolved =
                resolver.resolve(QualifyLeadWithAiRequest.builder()
                        .promptCode("LEAD_QUALIFY_REAL_ESTATE")
                        .variableKeys(List.of("budget", "location"))
                        .build());

        assertThat(resolved.promptCode()).isEqualTo("LEAD_QUALIFY_REAL_ESTATE");
        assertThat(resolved.variableKeys()).containsExactly("budget", "location");
    }

    @Test
    void shouldResolveFromEnabledIndustryPlugin() {
        when(marketplaceServiceClient.listInstallations())
                .thenReturn(ApiResponse.ok(List.of(PluginInstallationDto.builder()
                        .id(UUID.randomUUID())
                        .pluginKey("real-estate")
                        .pluginType(PluginTypeDto.INDUSTRY)
                        .status(PluginInstallationStatus.ENABLED)
                        .config(Map.of(
                                "qualificationPromptCode", "LEAD_QUALIFY_REAL_ESTATE",
                                "qualificationVariableKeys",
                                        List.of("budget", "location", "timeline")))
                        .build())));

        IndustryQualificationConfigResolver.Resolved resolved =
                resolver.resolve(QualifyLeadWithAiRequest.builder().build());

        assertThat(resolved.promptCode()).isEqualTo("LEAD_QUALIFY_REAL_ESTATE");
        assertThat(resolved.variableKeys()).containsExactly("budget", "location", "timeline");
    }

    @Test
    void shouldFailWhenPromptMissingAndNoPluginConfig() {
        when(marketplaceServiceClient.listInstallations()).thenReturn(ApiResponse.ok(List.of()));

        assertThatThrownBy(() -> resolver.resolve(QualifyLeadWithAiRequest.builder().build()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("promptCode is required");
    }

    @Test
    void shouldNotInventVariableKeysWhenOnlyPromptCodeProvided() {
        IndustryQualificationConfigResolver.Resolved resolved =
                resolver.resolve(QualifyLeadWithAiRequest.builder()
                        .promptCode("LEAD_QUALIFY_REAL_ESTATE")
                        .build());

        assertThat(resolved.promptCode()).isEqualTo("LEAD_QUALIFY_REAL_ESTATE");
        assertThat(resolved.variableKeys()).isEmpty();
    }

    @Test
    void shouldIgnoreNullPluginTypeEvenWithQualificationConfig() {
        when(marketplaceServiceClient.listInstallations())
                .thenReturn(ApiResponse.ok(List.of(PluginInstallationDto.builder()
                        .id(UUID.randomUUID())
                        .pluginKey("mystery")
                        .pluginType(null)
                        .status(PluginInstallationStatus.ENABLED)
                        .config(Map.of("qualificationPromptCode", "LEAD_QUALIFY_REAL_ESTATE"))
                        .build())));

        assertThatThrownBy(() -> resolver.resolve(QualifyLeadWithAiRequest.builder().build()))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("promptCode is required");
    }
}
