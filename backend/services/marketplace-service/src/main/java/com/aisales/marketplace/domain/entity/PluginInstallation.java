package com.aisales.marketplace.domain.entity;

import com.aisales.common.contracts.plugin.PluginInstallationStatus;
import com.aisales.common.exception.exception.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.HashMap;
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
@Table(name = "plugin_installation")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PluginInstallation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "plugin_key", nullable = false, length = 100)
    private String pluginKey;

    @Column(nullable = false, length = 50)
    private String version;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PluginInstallationStatus status = PluginInstallationStatus.DISABLED;

    @Builder.Default
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> config = new HashMap<>();

    @Column(name = "enabled_at")
    private Instant enabledAt;

    @Column(name = "disabled_at")
    private Instant disabledAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(name = "version_lock", nullable = false)
    @Builder.Default
    private Long versionLock = 0L;

    public void enable(Map<String, Object> configValue, UUID actor) {
        this.status = PluginInstallationStatus.ENABLED;
        if (configValue != null) {
            this.config = new HashMap<>(configValue);
        }
        this.enabledAt = Instant.now();
        this.disabledAt = null;
        touch(actor);
    }

    public void disable(UUID actor) {
        if (status == PluginInstallationStatus.DISABLED) {
            throw new ValidationException("Plugin is already disabled: " + pluginKey);
        }
        this.status = PluginInstallationStatus.DISABLED;
        this.disabledAt = Instant.now();
        touch(actor);
    }

    public void updateConfig(Map<String, Object> configValue, UUID actor) {
        if (configValue == null) {
            throw new ValidationException("config is required");
        }
        this.config = new HashMap<>(configValue);
        touch(actor);
    }

    private void touch(UUID actor) {
        this.updatedAt = Instant.now();
        this.updatedBy = actor;
    }
}
