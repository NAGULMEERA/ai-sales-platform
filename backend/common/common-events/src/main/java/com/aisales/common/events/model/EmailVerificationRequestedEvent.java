package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class EmailVerificationRequestedEvent extends BaseEvent {

    private String email;
    private String firstName;
    private String token;
    private String verificationLink;

    public static EmailVerificationRequestedEvent of(String tenantId, String userId, String email,
                                                     String firstName, String token, String verificationLink,
                                                     String correlationId) {
        EmailVerificationRequestedEvent event = new EmailVerificationRequestedEvent();
        event.init("EmailVerificationRequested", tenantId, userId, correlationId);
        event.email = email;
        event.firstName = firstName;
        event.token = token;
        event.verificationLink = verificationLink;
        return event;
    }
}
