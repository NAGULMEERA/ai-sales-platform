package com.aisales.common.events.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserUpdatedEvent extends BaseEvent {

    private String email;
    private String status;

    public static UserUpdatedEvent of(String tenantId, String userId, String email,
                                      String status, String correlationId) {
        UserUpdatedEvent event = new UserUpdatedEvent();
        event.init("UserUpdated", tenantId, userId, correlationId);
        event.setEmail(email);
        event.setStatus(status);
        return event;
    }
}
