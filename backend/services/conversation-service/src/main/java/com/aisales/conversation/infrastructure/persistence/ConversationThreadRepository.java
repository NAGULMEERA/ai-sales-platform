package com.aisales.conversation.infrastructure.persistence;

import com.aisales.conversation.domain.entity.ConversationThread;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationThreadRepository extends JpaRepository<ConversationThread, UUID> {

    Optional<ConversationThread> findByTenantIdAndIdAndDeletedAtIsNull(UUID tenantId, UUID id);

    Page<ConversationThread> findByTenantIdAndLeadIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID tenantId, UUID leadId, Pageable pageable);

    Page<ConversationThread> findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(
            UUID tenantId, Pageable pageable);
}
