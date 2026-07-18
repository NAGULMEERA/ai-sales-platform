package com.aisales.marketplace.infrastructure.persistence;

import com.aisales.marketplace.domain.entity.PluginInstallation;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PluginInstallationRepository extends JpaRepository<PluginInstallation, UUID> {

    Optional<PluginInstallation> findByTenantIdAndPluginKey(UUID tenantId, String pluginKey);

    Optional<PluginInstallation> findByTenantIdAndId(UUID tenantId, UUID id);

    List<PluginInstallation> findByTenantIdOrderByUpdatedAtDesc(UUID tenantId);
}
