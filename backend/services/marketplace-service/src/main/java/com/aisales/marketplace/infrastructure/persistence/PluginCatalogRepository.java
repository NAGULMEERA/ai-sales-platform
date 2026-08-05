package com.aisales.marketplace.infrastructure.persistence;

import com.aisales.common.contracts.plugin.PluginTypeDto;
import com.aisales.marketplace.domain.entity.PluginCatalogEntry;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PluginCatalogRepository extends JpaRepository<PluginCatalogEntry, UUID> {

    Optional<PluginCatalogEntry> findByPluginKeyAndAvailableTrue(String pluginKey);

    Optional<PluginCatalogEntry> findByPluginKey(String pluginKey);

    List<PluginCatalogEntry> findByPluginKeyIn(Collection<String> pluginKeys);

    Page<PluginCatalogEntry> findByAvailableTrueOrderByDisplayNameAsc(Pageable pageable);

    Page<PluginCatalogEntry> findByTypeAndAvailableTrueOrderByDisplayNameAsc(
            PluginTypeDto type, Pageable pageable);
}
