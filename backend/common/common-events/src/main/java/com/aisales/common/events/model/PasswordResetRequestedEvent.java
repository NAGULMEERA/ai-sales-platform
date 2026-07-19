package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PasswordResetRequestedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "PasswordResetRequested";

    private String email;
    private String firstName;
    private String token;
    private String resetLink;

    public static PasswordResetRequestedEvent of(String tenantId, String userId, String email,
                                                 String firstName, String token, String resetLink,
                                                 String correlationId) {
        PasswordResetRequestedEvent event = new PasswordResetRequestedEvent();
        event.init(EVENT_TYPE, tenantId, userId, correlationId);
        event.email = email;
        event.firstName = firstName;
        event.token = token;
        event.resetLink = resetLink;
        return event;
    }
}
