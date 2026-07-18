package com.aisales.ai.infrastructure.persistence;

import com.aisales.ai.domain.entity.KnowledgeChunk;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    List<KnowledgeChunk> findByTenantIdAndDocumentIdOrderByChunkIndexAsc(UUID tenantId, UUID documentId);

    @Modifying
    @Query("DELETE FROM KnowledgeChunk c WHERE c.tenantId = :tenantId AND c.documentId = :documentId")
    int deleteByTenantIdAndDocumentId(@Param("tenantId") UUID tenantId, @Param("documentId") UUID documentId);
}
