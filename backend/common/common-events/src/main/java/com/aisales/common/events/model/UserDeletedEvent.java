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

    public static UserDeletedEvent of(String tenantId, String userId, String correlationId) {
        UserDeletedEvent event = new UserDeletedEvent();
        event.init("UserDeleted", tenantId, userId, correlationId);
        return event;
    }
}
