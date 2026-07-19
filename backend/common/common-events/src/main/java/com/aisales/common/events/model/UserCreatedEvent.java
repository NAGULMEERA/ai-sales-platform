package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserCreatedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "UserCreated";

    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;

    public static UserCreatedEvent of(String tenantId, String userId, String email,
                                      String firstName, String lastName, Set<String> roles,
                                      String correlationId) {
        UserCreatedEvent event = new UserCreatedEvent();
        event.init(EVENT_TYPE, tenantId, userId, correlationId);
        event.email = email;
        event.firstName = firstName;
        event.lastName = lastName;
        event.roles = roles;
        return event;
    }
}
