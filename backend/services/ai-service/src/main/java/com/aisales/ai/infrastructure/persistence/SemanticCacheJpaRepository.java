package com.aisales.ai.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SemanticCacheJpaRepository extends JpaRepository<SemanticCacheEntry, UUID> {

    Optional<SemanticCacheEntry> findByTenantIdAndPromptScopeAndQueryHashAndModelUsed(
            UUID tenantId, String promptScope, String queryHash, String modelUsed);

    @Modifying
    @Query("DELETE FROM SemanticCacheEntry s WHERE s.tenantId = :tenantId")
    void deleteByTenantId(@Param("tenantId") UUID tenantId);

    long countByTenantId(UUID tenantId);

    @Query("SELECT COALESCE(SUM(s.hitCount), 0) FROM SemanticCacheEntry s WHERE s.tenantId = :tenantId")
    long sumHitCountByTenantId(@Param("tenantId") UUID tenantId);
}
