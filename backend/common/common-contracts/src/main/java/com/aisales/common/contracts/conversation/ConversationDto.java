package com.aisales.common.contracts.conversation;

import java.time.Instant;
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
public class ConversationDto {

    private UUID id;
    private UUID tenantId;
    private UUID organizationId;
    private UUID leadId;
    private UUID customerId;
    private UUID opportunityId;
    private ConversationChannel channel;
    private String subject;
    private ConversationStatus status;
    private String summary;
    private String aiSummary;
    private String sentiment;
    private String intent;
    private String classification;
    private String nextBestAction;
    private Instant lastMessageAt;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;
    private Long version;

    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();
}
