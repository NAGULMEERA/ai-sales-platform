package com.aisales.common.events.audit;

import com.aisales.common.core.audit.AuditRecorder;
import com.aisales.common.core.audit.CompositeAuditRecorder;
import com.aisales.common.core.audit.LoggingAuditRecorder;
import com.aisales.common.events.config.EventsAutoConfiguration;
import com.aisales.common.events.config.OutboxAutoConfiguration;
import com.aisales.common.events.publisher.EventPublisher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;

import java.util.List;

// Must run after the configurations that contribute EventPublisher beans, otherwise
// @ConditionalOnBean(EventPublisher) evaluates before they are registered.
@AutoConfiguration(after = {EventsAutoConfiguration.class, OutboxAutoConfiguration.class})
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
