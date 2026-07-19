package com.aisales.conversation.domain.entity;

import com.aisales.common.contracts.conversation.MessageContentType;
import com.aisales.common.contracts.conversation.MessageDeliveryStatus;
import com.aisales.common.contracts.conversation.MessageDirection;
import com.aisales.common.contracts.conversation.MessageSenderType;
import com.aisales.common.exception.exception.ValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversation_message")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessage {

    private static final Set<MessageDeliveryStatus> TERMINAL =
            Set.of(MessageDeliveryStatus.READ, MessageDeliveryStatus.FAILED);

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private UUID conversationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_type", nullable = false, length = 30)
    private MessageSenderType senderType;

    @Column(name = "sender_id")
    private UUID senderId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String body;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MessageDirection direction = MessageDirection.INBOUND;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false, length = 30)
    private MessageDeliveryStatus deliveryStatus = MessageDeliveryStatus.PENDING;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private MessageContentType contentType = MessageContentType.TEXT;

    @Column(name = "correlation_id", length = 255)
    private String correlationId;

    @Column(name = "media_id")
    private UUID mediaId;

    @Column(name = "media_url", length = 1024)
    private String mediaUrl;

    @Column(name = "media_content_type", length = 128)
    private String mediaContentType;

    @Builder.Default
    @Column(name = "retry_count", nullable = false)
    private Integer retryCount = 0;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by")
    private UUID createdBy;

    public void transitionStatus(MessageDeliveryStatus target, String reason) {
        if (target == null) {
            throw new ValidationException("Message status is required");
        }
        if (deliveryStatus == target) {
            return;
        }
        if (TERMINAL.contains(deliveryStatus) && target != MessageDeliveryStatus.FAILED) {
            throw new ValidationException("Message status is terminal: " + deliveryStatus);
        }
        switch (target) {
            case SENT -> {
                if (deliveryStatus != MessageDeliveryStatus.PENDING
                        && deliveryStatus != MessageDeliveryStatus.FAILED) {
                    throw new ValidationException("Cannot mark SENT from " + deliveryStatus);
                }
                this.deliveryStatus = MessageDeliveryStatus.SENT;
                this.failureReason = null;
            }
            case DELIVERED -> {
                if (deliveryStatus != MessageDeliveryStatus.SENT
                        && deliveryStatus != MessageDeliveryStatus.PENDING) {
                    throw new ValidationException("Cannot mark DELIVERED from " + deliveryStatus);
                }
                this.deliveryStatus = MessageDeliveryStatus.DELIVERED;
                this.deliveredAt = Instant.now();
            }
            case READ -> {
                if (deliveryStatus != MessageDeliveryStatus.DELIVERED
                        && deliveryStatus != MessageDeliveryStatus.SENT) {
                    throw new ValidationException("Cannot mark READ from " + deliveryStatus);
                }
                this.deliveryStatus = MessageDeliveryStatus.READ;
                if (this.deliveredAt == null) {
                    this.deliveredAt = Instant.now();
                }
                this.readAt = Instant.now();
            }
            case FAILED -> {
                this.deliveryStatus = MessageDeliveryStatus.FAILED;
                this.failureReason = reason;
            }
            case PENDING -> throw new ValidationException("Cannot transition back to PENDING");
            default -> throw new ValidationException("Unsupported status: " + target);
        }
    }

    public void markRetry() {
        if (deliveryStatus != MessageDeliveryStatus.FAILED
                && deliveryStatus != MessageDeliveryStatus.PENDING) {
            throw new ValidationException("Only failed or pending messages can be retried");
        }
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
        this.deliveryStatus = MessageDeliveryStatus.PENDING;
        this.failureReason = null;
    }
}
