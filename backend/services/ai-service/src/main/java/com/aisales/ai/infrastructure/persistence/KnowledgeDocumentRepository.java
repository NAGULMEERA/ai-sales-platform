package com.aisales.ai.infrastructure.persistence;

import com.aisales.ai.domain.entity.KnowledgeDocument;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    Optional<KnowledgeDocument> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT d FROM KnowledgeDocument d
            WHERE d.tenantId = :tenantId AND d.id = :id AND d.deletedAt IS NULL
            """)
    Optional<KnowledgeDocument> findByTenantIdAndIdForUpdate(
            @Param("tenantId") UUID tenantId, @Param("id") UUID id);

    List<KnowledgeDocument> findByTenantIdAndKnowledgeBaseIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID tenantId, UUID knowledgeBaseId);
}
