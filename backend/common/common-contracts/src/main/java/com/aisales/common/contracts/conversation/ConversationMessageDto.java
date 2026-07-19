package com.aisales.common.contracts.conversation;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationMessageDto {

    private UUID id;
    private UUID conversationId;
    private MessageSenderType senderType;
    private UUID senderId;
    private String body;
    private MessageDirection direction;
    private MessageDeliveryStatus deliveryStatus;
    private MessageContentType contentType;
    private String correlationId;
    private UUID mediaId;
    private String mediaUrl;
    private Integer retryCount;
    private String failureReason;
    private Instant deliveredAt;
    private Instant readAt;
    private Instant createdAt;

    @Builder.Default
    private List<ConversationAttachmentDto> attachments = new ArrayList<>();
}
