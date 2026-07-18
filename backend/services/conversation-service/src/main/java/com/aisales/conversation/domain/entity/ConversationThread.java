package com.aisales.conversation.domain.entity;

import com.aisales.common.contracts.conversation.ConversationChannel;
import com.aisales.common.contracts.conversation.ConversationStatus;
import com.aisales.common.exception.exception.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversation_thread")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationThread {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "organization_id")
    private UUID organizationId;

    @Column(name = "lead_id")
    private UUID leadId;

    @Column(name = "customer_id")
    private UUID customerId;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ConversationChannel channel = ConversationChannel.WEB;

    @Column(length = 255)
    private String subject;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConversationStatus status = ConversationStatus.OPEN;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Version
    @Column(nullable = false)
    @Builder.Default
    private Long version = 0L;

    @Column(name = "closed_at")
    private Instant closedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    public void assertOpen() {
        assertActive();
        if (status != ConversationStatus.OPEN) {
            throw new ValidationException("Conversation is closed");
        }
    }

    public void assertActive() {
        if (deletedAt != null) {
            throw new ValidationException("Conversation is deleted");
        }
    }

    public void close(UUID actor) {
        assertActive();
        if (status == ConversationStatus.CLOSED) {
            return;
        }
        this.status = ConversationStatus.CLOSED;
        this.closedAt = Instant.now();
        touch(actor);
    }

    public void touch(UUID actor) {
        this.updatedAt = Instant.now();
        this.updatedBy = actor;
    }
}
