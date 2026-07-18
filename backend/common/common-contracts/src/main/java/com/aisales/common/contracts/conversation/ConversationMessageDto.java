package com.aisales.common.contracts.conversation;

import java.time.Instant;
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
    private Instant createdAt;
}
