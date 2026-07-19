package com.aisales.common.contracts.conversation;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateConversationRequest {

    /** Preferred business link for sales journeys. */
    private UUID leadId;

    private UUID customerId;

    private UUID opportunityId;

    @NotNull
    private ConversationChannel channel;

    @Size(max = 255)
    private String subject;

    /** Optional first message body when starting the conversation. */
    @Size(max = 8000)
    private String initialMessage;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
