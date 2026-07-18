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
public class ConversationDto {

    private UUID id;
    private UUID tenantId;
    private UUID organizationId;
    private UUID leadId;
    private UUID customerId;
    private ConversationChannel channel;
    private String subject;
    private ConversationStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant closedAt;
    private Long version;
}
