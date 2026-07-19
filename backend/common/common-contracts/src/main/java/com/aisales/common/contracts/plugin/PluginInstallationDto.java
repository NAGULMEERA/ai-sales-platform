package com.aisales.common.contracts.plugin;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginInstallationDto {

    private UUID id;
    private UUID tenantId;
    private String pluginKey;
    /** Catalog type when known (INDUSTRY / CAPABILITY). */
    private PluginTypeDto pluginType;
    private String version;
    private PluginInstallationStatus status;
    @Builder.Default
    private Map<String, Object> config = new HashMap<>();
    private Instant enabledAt;
    private Instant disabledAt;
    private Instant createdAt;
    private Instant updatedAt;
}
