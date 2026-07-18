package com.aisales.media.infrastructure.persistence;

import com.aisales.media.domain.entity.MediaObject;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MediaObjectRepository extends JpaRepository<MediaObject, UUID> {

    Optional<MediaObject> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);
}
