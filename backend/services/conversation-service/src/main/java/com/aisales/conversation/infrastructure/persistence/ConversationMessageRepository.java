package com.aisales.conversation.infrastructure.persistence;

import com.aisales.conversation.domain.entity.ConversationMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    List<ConversationMessage> findByTenantIdAndConversationIdOrderByCreatedAtAsc(
            UUID tenantId, UUID conversationId);

    Optional<ConversationMessage> findByTenantIdAndIdAndConversationId(
            UUID tenantId, UUID id, UUID conversationId);
}

