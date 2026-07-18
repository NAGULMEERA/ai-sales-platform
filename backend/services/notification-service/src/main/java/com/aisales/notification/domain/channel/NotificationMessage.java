package com.aisales.notification.domain.channel;

import java.util.Map;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class NotificationMessage {
    String tenantId;
    String recipient;
    String templateCode;
    Map<String, String> variables;
    String correlationId;
}
