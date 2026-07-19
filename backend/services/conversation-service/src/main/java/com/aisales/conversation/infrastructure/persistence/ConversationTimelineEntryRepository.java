package com.aisales.conversation.infrastructure.persistence;

import com.aisales.conversation.domain.entity.ConversationTimelineEntry;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationTimelineEntryRepository
        extends JpaRepository<ConversationTimelineEntry, UUID> {

    List<ConversationTimelineEntry> findByTenantIdAndConversationIdOrderByOccurredAtAsc(
            UUID tenantId, UUID conversationId);
}
