package com.aisales.ai.infrastructure.persistence;

import com.aisales.ai.domain.entity.PromptVersionEntity;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PromptVersionRepository extends JpaRepository<PromptVersionEntity, UUID> {

    Optional<PromptVersionEntity> findByTenantIdAndPromptIdAndVersionNumber(
            UUID tenantId, UUID promptId, Integer versionNumber);

    List<PromptVersionEntity> findByTenantIdAndPromptIdOrderByVersionNumberDesc(
            UUID tenantId, UUID promptId);

    @Query("""
            SELECT COALESCE(MAX(v.versionNumber), 0)
            FROM PromptVersionEntity v
            WHERE v.tenantId = :tenantId AND v.promptId = :promptId
            """)
    int findMaxVersion(@Param("tenantId") UUID tenantId, @Param("promptId") UUID promptId);
}
