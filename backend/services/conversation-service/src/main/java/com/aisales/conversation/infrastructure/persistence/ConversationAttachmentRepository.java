package com.aisales.conversation.infrastructure.persistence;

import com.aisales.conversation.domain.entity.ConversationAttachment;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationAttachmentRepository extends JpaRepository<ConversationAttachment, UUID> {

    List<ConversationAttachment> findByTenantIdAndMessageIdOrderByCreatedAtAsc(
            UUID tenantId, UUID messageId);

    List<ConversationAttachment> findByTenantIdAndConversationIdOrderByCreatedAtAsc(
            UUID tenantId, UUID conversationId);
}
