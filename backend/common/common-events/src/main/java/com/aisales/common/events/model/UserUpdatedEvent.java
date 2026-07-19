package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserUpdatedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "UserUpdated";

    private String email;
    private String status;

    public static UserUpdatedEvent of(String tenantId, String userId, String email,
                                      String status, String correlationId) {
        UserUpdatedEvent event = new UserUpdatedEvent();
        event.init(EVENT_TYPE, tenantId, userId, correlationId);
        event.email = email;
        event.status = status;
        return event;
    }
}
