package com.aisales.common.events.audit;

import com.aisales.common.events.model.BaseEvent;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
public class AuditRecordedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "AuditRecorded";

    private String userId;
    private String action;
    private String resourceType;
    private String resourceId;
    private String detailsJson;

    public static AuditRecordedEvent of(
            String tenantId,
            String userId,
            String action,
            String resourceType,
            String resourceId,
            String correlationId,
            String detailsJson) {
        AuditRecordedEvent event = AuditRecordedEvent.builder()
                .userId(userId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .detailsJson(detailsJson)
                .build();
        event.init(EVENT_TYPE, tenantId, resourceId, correlationId);
        return event;
    }
}
