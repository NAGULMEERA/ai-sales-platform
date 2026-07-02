package com.aisales.common.events.audit;

import com.aisales.common.core.audit.AuditRecorder;
import com.aisales.common.core.audit.CompositeAuditRecorder;
import com.aisales.common.core.audit.LoggingAuditRecorder;
import com.aisales.common.events.publisher.EventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.List;

@AutoConfiguration
@ConditionalOnProperty(name = "aisales.audit.publish-events", havingValue = "true")
@ConditionalOnBean(EventPublisher.class)
public class EventAuditAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(AuditRecorder.class)
    public AuditRecorder eventPublishingAuditRecorder(
            EventPublisher eventPublisher,
            @Value("${aisales.events.default-topic:aisales-events}") String defaultTopic) {
        return new CompositeAuditRecorder(List.of(
                new LoggingAuditRecorder(),
                new EventPublishingAuditRecorder(eventPublisher, defaultTopic)));
    }
}
