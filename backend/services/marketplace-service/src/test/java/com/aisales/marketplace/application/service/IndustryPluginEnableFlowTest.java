package com.aisales.marketplace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.plugin.EnablePluginRequest;
import com.aisales.common.contracts.plugin.PluginInstallationDto;
import com.aisales.common.contracts.plugin.PluginInstallationStatus;
import com.aisales.common.contracts.plugin.PluginTypeDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.PluginEnabledEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.marketplace.application.mapper.PluginMapper;
import com.aisales.marketplace.domain.entity.PluginCatalogEntry;
import com.aisales.marketplace.domain.entity.PluginInstallation;
import com.aisales.marketplace.infrastructure.configuration.PlatformVersionProperties;
import com.aisales.marketplace.infrastructure.persistence.PluginCatalogRepository;
import com.aisales.marketplace.infrastructure.persistence.PluginInstallationRepository;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Sprint 7: same marketplace enable API for real-estate and automobile industry plugins.
 */
@ExtendWith(MockitoExtension.class)
class IndustryPluginEnableFlowTest {

    @Mock private PluginCatalogRepository catalogRepository;
    @Mock private PluginInstallationRepository installationRepository;
    @Mock private EventPublisher eventPublisher;

    private PluginRegistryService service;
    private UUID tenantId;

    @BeforeEach
    void setUp() {
        tenantId = UUID.randomUUID();
        TenantContext.setTenantId(tenantId.toString());
        TenantContext.setUserId(UUID.randomUUID().toString());
        PlatformVersionProperties platformVersion = new PlatformVersionProperties();
        platformVersion.setVersion("1.0.0");
        service = new PluginRegistryService(
                catalogRepository,
                installationRepository,
                new PluginMapper(),
                eventPublisher,
                platformVersion);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldEnableRealEstateIndustryPluginOnSameApi() {
        PluginInstallationDto dto = enableIndustryPlugin(
                "real-estate",
                "REAL_ESTATE",
                Map.of(
                        "defaultPipelineCode", "REAL_ESTATE_SALES_V1",
                        "defaultFollowupType", "VISIT_FOLLOWUP"));

        assertThat(dto.getPluginKey()).isEqualTo("real-estate");
        assertThat(dto.getStatus()).isEqualTo(PluginInstallationStatus.ENABLED);
        assertThat(dto.getConfig())
                .containsEntry("defaultPipelineCode", "REAL_ESTATE_SALES_V1")
                .containsEntry("defaultFollowupType", "VISIT_FOLLOWUP");

        ArgumentCaptor<PluginEnabledEvent> captor = ArgumentCaptor.forClass(PluginEnabledEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getPluginKey()).isEqualTo("real-estate");
        assertThat(captor.getValue().getPluginType()).isEqualTo("INDUSTRY");
        assertThat(captor.getValue().getEventType()).isEqualTo("PluginEnabled");
    }

    @Test
    void shouldEnableAutomobileIndustryPluginOnSameApi() {
        PluginInstallationDto dto = enableIndustryPlugin(
                "automobile",
                "AUTOMOBILE",
                Map.of(
                        "defaultPipelineCode", "AUTOMOBILE_SALES_V1",
                        "defaultFollowupType", "TEST_DRIVE_FOLLOWUP"));

        assertThat(dto.getPluginKey()).isEqualTo("automobile");
        assertThat(dto.getStatus()).isEqualTo(PluginInstallationStatus.ENABLED);
        assertThat(dto.getConfig())
                .containsEntry("defaultPipelineCode", "AUTOMOBILE_SALES_V1")
                .containsEntry("defaultFollowupType", "TEST_DRIVE_FOLLOWUP");

        ArgumentCaptor<PluginEnabledEvent> captor = ArgumentCaptor.forClass(PluginEnabledEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getPluginKey()).isEqualTo("automobile");
        assertThat(captor.getValue().getPluginType()).isEqualTo("INDUSTRY");
    }

    @Test
    void shouldEnableNaturalFarmingIndustryPluginOnSameApi() {
        PluginInstallationDto dto = enableIndustryPlugin(
                "natural-farming",
                "NATURAL_FARMING",
                Map.of(
                        "defaultPipelineCode", "NATURAL_FARMING_SALES_V1",
                        "defaultFollowupType", "DELIVERY_FOLLOWUP",
                        "qualificationPromptCode", "LEAD_QUALIFY_NATURAL_FARMING"));

        assertThat(dto.getPluginKey()).isEqualTo("natural-farming");
        assertThat(dto.getStatus()).isEqualTo(PluginInstallationStatus.ENABLED);
        assertThat(dto.getConfig())
                .containsEntry("defaultPipelineCode", "NATURAL_FARMING_SALES_V1")
                .containsEntry("defaultFollowupType", "DELIVERY_FOLLOWUP")
                .containsEntry("qualificationPromptCode", "LEAD_QUALIFY_NATURAL_FARMING");

        ArgumentCaptor<PluginEnabledEvent> captor = ArgumentCaptor.forClass(PluginEnabledEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getPluginKey()).isEqualTo("natural-farming");
        assertThat(captor.getValue().getPluginType()).isEqualTo("INDUSTRY");
    }

    @Test
    void shouldEnableBothIndustryPluginsIndependentlyForSameTenant() {
        stubCatalogAndInstall("real-estate", "REAL_ESTATE", Map.of("defaultFollowupType", "VISIT_FOLLOWUP"));
        PluginInstallationDto re = service.enable("real-estate", EnablePluginRequest.builder().build());

        stubCatalogAndInstall("automobile", "AUTOMOBILE", Map.of("defaultFollowupType", "TEST_DRIVE_FOLLOWUP"));
        PluginInstallationDto auto = service.enable("automobile", EnablePluginRequest.builder().build());

        assertThat(re.getPluginKey()).isEqualTo("real-estate");
        assertThat(auto.getPluginKey()).isEqualTo("automobile");
        assertThat(re.getStatus()).isEqualTo(PluginInstallationStatus.ENABLED);
        assertThat(auto.getStatus()).isEqualTo(PluginInstallationStatus.ENABLED);
        assertThat(re.getTenantId()).isEqualTo(tenantId);
        assertThat(auto.getTenantId()).isEqualTo(tenantId);
    }

    private PluginInstallationDto enableIndustryPlugin(
            String pluginKey, String industryCode, Map<String, Object> defaultConfig) {
        stubCatalogAndInstall(pluginKey, industryCode, defaultConfig);
        return service.enable(pluginKey, EnablePluginRequest.builder().build());
    }

    private void stubCatalogAndInstall(
            String pluginKey, String industryCode, Map<String, Object> defaultConfig) {
        PluginCatalogEntry catalog = PluginCatalogEntry.builder()
                .id(UUID.randomUUID())
                .pluginKey(pluginKey)
                .type(PluginTypeDto.INDUSTRY)
                .version("1.0.0")
                .minPlatformVersion("1.0.0")
                .displayName(pluginKey)
                .description("Industry metadata plugin")
                .capabilities(List.of("industry." + industryCode.toLowerCase()))
                .industryCode(industryCode)
                .configSchemaJson("{}")
                .defaultConfig(new HashMap<>(defaultConfig))
                .metadata(new HashMap<>(Map.of(
                        "ownsMicroservice", false,
                        "leadSubtype", false,
                        "industryConversationType", false)))
                .available(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(catalogRepository.findByPluginKeyAndAvailableTrue(pluginKey))
                .thenReturn(Optional.of(catalog));
        when(installationRepository.findByTenantIdAndPluginKey(tenantId, pluginKey))
                .thenReturn(Optional.empty());
        when(installationRepository.saveAndFlush(any(PluginInstallation.class))).thenAnswer(inv -> {
            PluginInstallation installation = inv.getArgument(0);
            installation.setId(UUID.randomUUID());
            return installation;
        });
    }
}
