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
public class ConversationParticipantDto {

    private UUID id;
    private UUID conversationId;
    private ConversationParticipantRole role;
    private UUID participantId;
    private String displayName;
    private Instant joinedAt;
    private Instant leftAt;
}
