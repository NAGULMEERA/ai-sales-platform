package com.aisales.common.contracts.lead;

import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** Append-only journey entry (never overwritten). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeadTimelineEntryDto {
    private UUID id;
    private UUID leadId;
    private String eventType;
    private String description;
    private UUID actorId;
    private Instant occurredAt;
}
