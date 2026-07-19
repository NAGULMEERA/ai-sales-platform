package com.aisales.common.contracts.plugin;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
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
public class PluginCatalogDto {

    private UUID id;
    private String pluginKey;
    private PluginTypeDto type;
    private String version;
    /** Minimum platform semver required to enable this plugin. */
    private String minPlatformVersion;
    private String displayName;
    private String description;
    @Builder.Default
    private List<String> capabilities = new ArrayList<>();
    private String industryCode;
    private String configSchemaJson;
    @Builder.Default
    private Map<String, Object> defaultConfig = new HashMap<>();
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
    private boolean available;
    private Instant createdAt;
    private Instant updatedAt;
}
