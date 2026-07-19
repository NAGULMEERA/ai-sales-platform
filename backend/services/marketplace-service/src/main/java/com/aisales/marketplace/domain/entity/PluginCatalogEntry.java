package com.aisales.marketplace.domain.entity;

import com.aisales.common.contracts.plugin.PluginTypeDto;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "plugin_catalog")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginCatalogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "plugin_key", nullable = false, unique = true, length = 100)
    private String pluginKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "plugin_type", nullable = false, length = 30)
    private PluginTypeDto type;

    @Column(nullable = false, length = 50)
    private String version;

    /** Minimum platform semver required to enable this plugin. */
    @Builder.Default
    @Column(name = "min_platform_version", nullable = false, length = 50)
    private String minPlatformVersion = "1.0.0";

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(length = 2000)
    private String description;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<String> capabilities = new ArrayList<>();

    @Column(name = "industry_code", length = 50)
    private String industryCode;

    @Builder.Default
    @Column(name = "config_schema_json", nullable = false, columnDefinition = "TEXT")
    private String configSchemaJson = "{}";

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_config", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> defaultConfig = new HashMap<>();

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> metadata = new HashMap<>();

    @Builder.Default
    @Column(nullable = false)
    private boolean available = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
