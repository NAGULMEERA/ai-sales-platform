package com.aisales.common.contracts.customer;

import java.time.Instant;
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
public class CustomerTimelineEntryDto {

    private UUID id;
    private UUID customerId;
    private String eventType;
    private String summary;
    private Map<String, Object> eventData;
    private Instant occurredAt;
    private UUID createdBy;
}
