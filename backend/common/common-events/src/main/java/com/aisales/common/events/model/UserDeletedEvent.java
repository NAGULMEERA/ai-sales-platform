package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserDeletedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "UserDeleted";

    public static UserDeletedEvent of(String tenantId, String userId, String correlationId) {
        UserDeletedEvent event = new UserDeletedEvent();
        event.init(EVENT_TYPE, tenantId, userId, correlationId);
        return event;
    }
}
