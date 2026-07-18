package com.aisales.ai.infrastructure.persistence;

import com.aisales.ai.domain.entity.KnowledgeDocument;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    Optional<KnowledgeDocument> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    List<KnowledgeDocument> findByTenantIdAndKnowledgeBaseIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID tenantId, UUID knowledgeBaseId);
}
