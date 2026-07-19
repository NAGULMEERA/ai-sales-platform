package com.aisales.conversation.infrastructure.persistence;

import com.aisales.conversation.domain.entity.ConversationParticipant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationParticipantRepository extends JpaRepository<ConversationParticipant, UUID> {

    List<ConversationParticipant> findByTenantIdAndConversationIdAndLeftAtIsNullOrderByJoinedAtAsc(
            UUID tenantId, UUID conversationId);

    Optional<ConversationParticipant> findByTenantIdAndIdAndConversationId(
            UUID tenantId, UUID id, UUID conversationId);
}
