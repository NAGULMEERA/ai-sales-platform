package com.aisales.ai.infrastructure.persistence;

import com.aisales.ai.domain.entity.KnowledgeBase;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, UUID> {

    Optional<KnowledgeBase> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    boolean existsByTenantIdAndCodeAndDeletedAtIsNull(UUID tenantId, String code);

    Page<KnowledgeBase> findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(
            UUID tenantId, Pageable pageable);
}
