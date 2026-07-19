package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmailVerifiedEvent extends BaseEvent {

    public static final String EVENT_TYPE = "EmailVerified";

    private String email;

    public static EmailVerifiedEvent of(String tenantId, String userId, String email, String correlationId) {
        EmailVerifiedEvent event = new EmailVerifiedEvent();
        event.init(EVENT_TYPE, tenantId, userId, correlationId);
        event.email = email;
        return event;
    }
}
