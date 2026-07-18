package com.aisales.notification.application.channel;

import com.aisales.common.contracts.notification.SendTransactionalEmailRequest;
import com.aisales.common.observability.metrics.MetricNames;
import com.aisales.common.observability.metrics.PlatformMetrics;
import com.aisales.notification.application.service.EmailDeliveryService;
import com.aisales.notification.domain.channel.NotificationChannel;
import com.aisales.notification.domain.channel.NotificationChannelType;
import com.aisales.notification.domain.channel.NotificationMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class EmailNotificationChannel implements NotificationChannel {

    private final EmailDeliveryService emailDeliveryService;
    private final ObjectProvider<PlatformMetrics> platformMetrics;

    @Override
    public NotificationChannelType type() {
        return NotificationChannelType.EMAIL;
    }

    @Override
    public void send(NotificationMessage message) {
        PlatformMetrics metrics = platformMetrics.getIfAvailable();
        try {
            emailDeliveryService.sendTransactionalEmail(SendTransactionalEmailRequest.builder()
                    .tenantId(message.getTenantId())
                    .recipientEmail(message.getRecipient())
                    .templateCode(message.getTemplateCode())
                    .variables(message.getVariables())
                    .correlationId(message.getCorrelationId())
                    .build());
            if (metrics != null) {
                metrics.incrementBusinessMetric(MetricNames.NOTIFICATION_DELIVERY, message.getTenantId(),
                        "channel", type().name(), "outcome", "success");
            }
        } catch (RuntimeException ex) {
            if (metrics != null) {
                metrics.incrementBusinessMetric(MetricNames.NOTIFICATION_DELIVERY, message.getTenantId(),
                        "channel", type().name(), "outcome", "failure");
            }
            throw ex;
        }
    }
}
