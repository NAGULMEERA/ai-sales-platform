package com.aisales.common.events.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmailVerifiedEvent extends BaseEvent {

    private String email;

    public static EmailVerifiedEvent of(String tenantId, String userId, String email, String correlationId) {
        EmailVerifiedEvent event = new EmailVerifiedEvent();
        event.init("EmailVerified", tenantId, userId, correlationId);
        event.setEmail(email);
        return event;
    }
}
