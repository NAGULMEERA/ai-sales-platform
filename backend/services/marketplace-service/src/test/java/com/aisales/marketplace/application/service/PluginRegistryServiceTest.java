package com.aisales.marketplace.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.aisales.common.contracts.plugin.EnablePluginRequest;
import com.aisales.common.contracts.plugin.PluginCatalogDto;
import com.aisales.common.contracts.plugin.PluginInstallationDto;
import com.aisales.common.contracts.plugin.PluginInstallationStatus;
import com.aisales.common.contracts.plugin.PluginTypeDto;
import com.aisales.common.core.util.TenantContext;
import com.aisales.common.events.model.PluginDisabledEvent;
import com.aisales.common.events.model.PluginEnabledEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.aisales.common.exception.exception.NotFoundException;
import com.aisales.marketplace.application.mapper.PluginMapper;
import com.aisales.marketplace.domain.entity.PluginCatalogEntry;
import com.aisales.marketplace.domain.entity.PluginInstallation;
import com.aisales.marketplace.infrastructure.persistence.PluginCatalogRepository;
import com.aisales.marketplace.infrastructure.persistence.PluginInstallationRepository;
import java.time.Instant;
import java.util.HashMap;
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

@ExtendWith(MockitoExtension.class)
class PluginRegistryServiceTest {

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
        service = new PluginRegistryService(
                catalogRepository, installationRepository, new PluginMapper(), eventPublisher);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void shouldEnablePluginAndPublishEvent() {
        PluginCatalogEntry catalog = catalog("email-channel", PluginTypeDto.CAPABILITY);
        when(catalogRepository.findByPluginKeyAndAvailableTrue("email-channel"))
                .thenReturn(Optional.of(catalog));
        when(installationRepository.findByTenantIdAndPluginKey(tenantId, "email-channel"))
                .thenReturn(Optional.empty());
        when(installationRepository.saveAndFlush(any(PluginInstallation.class))).thenAnswer(inv -> {
            PluginInstallation i = inv.getArgument(0);
            i.setId(UUID.randomUUID());
            return i;
        });

        PluginInstallationDto dto = service.enable("email-channel", EnablePluginRequest.builder()
                .config(Map.of("fromAddress", "sales@example.com"))
                .build());

        assertThat(dto.getStatus()).isEqualTo(PluginInstallationStatus.ENABLED);
        assertThat(dto.getConfig()).containsEntry("fromAddress", "sales@example.com");
        ArgumentCaptor<PluginEnabledEvent> captor = ArgumentCaptor.forClass(PluginEnabledEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getPluginKey()).isEqualTo("email-channel");
    }

    @Test
    void shouldDisablePluginAndPublishEvent() {
        PluginCatalogEntry catalog = catalog("email-channel", PluginTypeDto.CAPABILITY);
        when(catalogRepository.findByPluginKeyAndAvailableTrue("email-channel"))
                .thenReturn(Optional.of(catalog));
        PluginInstallation installation = PluginInstallation.builder()
                .id(UUID.randomUUID())
                .tenantId(tenantId)
                .pluginKey("email-channel")
                .version("1.0.0")
                .status(PluginInstallationStatus.ENABLED)
                .config(new HashMap<>())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        when(installationRepository.findByTenantIdAndPluginKey(tenantId, "email-channel"))
                .thenReturn(Optional.of(installation));
        when(installationRepository.save(any(PluginInstallation.class))).thenAnswer(inv -> inv.getArgument(0));

        PluginInstallationDto dto = service.disable("email-channel");

        assertThat(dto.getStatus()).isEqualTo(PluginInstallationStatus.DISABLED);
        ArgumentCaptor<PluginDisabledEvent> captor = ArgumentCaptor.forClass(PluginDisabledEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("PluginDisabled");
    }

    @Test
    void shouldGetCatalogEntry() {
        when(catalogRepository.findByPluginKeyAndAvailableTrue("real-estate"))
                .thenReturn(Optional.of(catalog("real-estate", PluginTypeDto.INDUSTRY)));

        PluginCatalogDto dto = service.getCatalogEntry("real-estate");

        assertThat(dto.getType()).isEqualTo(PluginTypeDto.INDUSTRY);
        assertThat(dto.getIndustryCode()).isEqualTo("REAL_ESTATE");
    }

    @Test
    void shouldRejectUnknownPlugin() {
        when(catalogRepository.findByPluginKeyAndAvailableTrue("missing"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.enable("missing", EnablePluginRequest.builder().build()))
                .isInstanceOf(NotFoundException.class);
    }

    private PluginCatalogEntry catalog(String key, PluginTypeDto type) {
        return PluginCatalogEntry.builder()
                .id(UUID.randomUUID())
                .pluginKey(key)
                .type(type)
                .version("1.0.0")
                .displayName(key)
                .description("test")
                .capabilities(java.util.List.of("test"))
                .industryCode(type == PluginTypeDto.INDUSTRY ? "REAL_ESTATE" : null)
                .configSchemaJson("{}")
                .defaultConfig(new HashMap<>(Map.of("fromAddress", "")))
                .metadata(new HashMap<>())
                .available(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }
}
