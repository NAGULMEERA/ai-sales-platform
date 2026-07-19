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
public class CustomerInteractionDto {

    private UUID id;
    private UUID customerId;
    private String interactionType;
    private String channel;
    private String summary;
    private Map<String, Object> details;
    private Instant occurredAt;
}
