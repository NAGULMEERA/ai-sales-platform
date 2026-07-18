package com.aisales.ai.infrastructure.persistence;

import com.aisales.ai.domain.entity.PromptTemplate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PromptTemplateRepository extends JpaRepository<PromptTemplate, UUID> {

    Optional<PromptTemplate> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    Optional<PromptTemplate> findByTenantIdAndCodeAndDeletedAtIsNull(UUID tenantId, String code);

    Page<PromptTemplate> findByTenantIdAndDeletedAtIsNullOrderByUpdatedAtDesc(
            UUID tenantId, Pageable pageable);

    boolean existsByTenantIdAndCodeAndDeletedAtIsNull(UUID tenantId, String code);
}
