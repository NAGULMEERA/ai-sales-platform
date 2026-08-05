package com.aisales.conversation.infrastructure.persistence;

import com.aisales.conversation.domain.entity.ConversationMessage;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    List<ConversationMessage> findByTenantIdAndConversationIdOrderByCreatedAtAsc(
            UUID tenantId, UUID conversationId);

    /**
     * Recent messages newest-first (use with {@link Pageable} for a bounded AI context window).
     */
    @Query("""
            SELECT m FROM ConversationMessage m
            WHERE m.tenantId = :tenantId AND m.conversationId = :conversationId
            ORDER BY m.createdAt DESC
            """)
    List<ConversationMessage> findRecentByTenantIdAndConversationId(
            @Param("tenantId") UUID tenantId,
            @Param("conversationId") UUID conversationId,
            Pageable pageable);

    Optional<ConversationMessage> findByTenantIdAndIdAndConversationId(
            UUID tenantId, UUID id, UUID conversationId);
}

