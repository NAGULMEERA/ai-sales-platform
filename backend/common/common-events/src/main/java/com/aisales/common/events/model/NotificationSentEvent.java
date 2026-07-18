package com.aisales.common.events.model;

import lombok.Getter;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationSentEvent extends BaseEvent {

    private String channel;
    private String recipient;
    private String templateId;

    public static NotificationSentEvent of(String tenantId, String notificationId, String channel,
                                           String recipient, String templateId, String correlationId) {
        NotificationSentEvent event = new NotificationSentEvent();
        event.init("NotificationSent", tenantId, notificationId, correlationId);
        event.channel = channel;
        event.recipient = recipient;
        event.templateId = templateId;
        return event;
    }
}
