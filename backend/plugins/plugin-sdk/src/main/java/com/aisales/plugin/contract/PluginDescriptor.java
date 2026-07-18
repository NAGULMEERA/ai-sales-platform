package com.aisales.plugin.contract;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Immutable metadata contract for plugin authors.
 * Plugins contribute configuration and capability declarations — not business logic.
 */
public final class PluginDescriptor {

    private final String pluginKey;
    private final PluginType type;
    private final String version;
    private final String displayName;
    private final String description;
    private final List<String> capabilities;
    private final String industryCode;
    private final String configSchemaJson;
    private final Map<String, Object> defaultConfig;
    private final Map<String, Object> metadata;

    private PluginDescriptor(Builder builder) {
        this.pluginKey = Objects.requireNonNull(builder.pluginKey, "pluginKey");
        this.type = Objects.requireNonNull(builder.type, "type");
        this.version = Objects.requireNonNull(builder.version, "version");
        this.displayName = Objects.requireNonNull(builder.displayName, "displayName");
        this.description = builder.description;
        this.capabilities = List.copyOf(builder.capabilities);
        this.industryCode = builder.industryCode;
        this.configSchemaJson = builder.configSchemaJson;
        this.defaultConfig = Collections.unmodifiableMap(new LinkedHashMap<>(builder.defaultConfig));
        this.metadata = Collections.unmodifiableMap(new LinkedHashMap<>(builder.metadata));
        if (type == PluginType.INDUSTRY && (industryCode == null || industryCode.isBlank())) {
            throw new IllegalArgumentException("industryCode is required for INDUSTRY plugins");
        }
    }

    public String getPluginKey() {
        return pluginKey;
    }

    public PluginType getType() {
        return type;
    }

    public String getVersion() {
        return version;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getCapabilities() {
        return capabilities;
    }

    public String getIndustryCode() {
        return industryCode;
    }

    public String getConfigSchemaJson() {
        return configSchemaJson;
    }

    public Map<String, Object> getDefaultConfig() {
        return defaultConfig;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private String pluginKey;
        private PluginType type;
        private String version;
        private String displayName;
        private String description;
        private List<String> capabilities = List.of();
        private String industryCode;
        private String configSchemaJson = "{}";
        private Map<String, Object> defaultConfig = new LinkedHashMap<>();
        private Map<String, Object> metadata = new LinkedHashMap<>();

        public Builder pluginKey(String pluginKey) {
            this.pluginKey = pluginKey;
            return this;
        }

        public Builder type(PluginType type) {
            this.type = type;
            return this;
        }

        public Builder version(String version) {
            this.version = version;
            return this;
        }

        public Builder displayName(String displayName) {
            this.displayName = displayName;
            return this;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Builder capabilities(List<String> capabilities) {
            this.capabilities = capabilities != null ? capabilities : List.of();
            return this;
        }

        public Builder industryCode(String industryCode) {
            this.industryCode = industryCode;
            return this;
        }

        public Builder configSchemaJson(String configSchemaJson) {
            this.configSchemaJson = configSchemaJson != null ? configSchemaJson : "{}";
            return this;
        }

        public Builder defaultConfig(Map<String, Object> defaultConfig) {
            this.defaultConfig = defaultConfig != null ? new LinkedHashMap<>(defaultConfig) : new LinkedHashMap<>();
            return this;
        }

        public Builder metadata(Map<String, Object> metadata) {
            this.metadata = metadata != null ? new LinkedHashMap<>(metadata) : new LinkedHashMap<>();
            return this;
        }

        public PluginDescriptor build() {
            return new PluginDescriptor(this);
        }
    }
}
