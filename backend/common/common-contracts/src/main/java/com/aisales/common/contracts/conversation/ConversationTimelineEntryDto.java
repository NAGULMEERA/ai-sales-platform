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
public class ConversationTimelineEntryDto {

    private UUID id;
    private String entryType;
    private String summary;
    private UUID actorId;
    private Instant occurredAt;

    @Builder.Default
    private Map<String, Object> details = new HashMap<>();
}
