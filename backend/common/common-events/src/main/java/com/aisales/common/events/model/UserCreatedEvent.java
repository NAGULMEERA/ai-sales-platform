package com.aisales.common.events.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Set;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserCreatedEvent extends BaseEvent {

    private String email;
    private String firstName;
    private String lastName;
    private Set<String> roles;

    public static UserCreatedEvent of(String tenantId, String userId, String email,
                                      String firstName, String lastName, Set<String> roles,
                                      String correlationId) {
        UserCreatedEvent event = new UserCreatedEvent();
        event.init("UserCreated", tenantId, userId, correlationId);
        event.setEmail(email);
        event.setFirstName(firstName);
        event.setLastName(lastName);
        event.setRoles(roles);
        return event;
    }
}
