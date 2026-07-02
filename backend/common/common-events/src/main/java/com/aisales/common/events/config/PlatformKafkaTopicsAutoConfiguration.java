package com.aisales.common.events.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;

@AutoConfiguration
@ConditionalOnClass(NewTopic.class)
@ConditionalOnProperty(name = "aisales.events.kafka.auto-create-topics", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(PlatformKafkaTopicsProperties.class)
public class PlatformKafkaTopicsAutoConfiguration {

    @Bean
    public NewTopic aisalesEventsTopic(PlatformKafkaTopicsProperties properties) {
        return buildTopic("aisales-events", properties, 604800000L);
    }

    @Bean
    public NewTopic leadEventsTopic(PlatformKafkaTopicsProperties properties) {
        return buildTopic("lead-events", properties, null);
    }

    @Bean
    public NewTopic conversationEventsTopic(PlatformKafkaTopicsProperties properties) {
        return buildTopic("conversation-events", properties, null);
    }

    @Bean
    public NewTopic appointmentEventsTopic(PlatformKafkaTopicsProperties properties) {
        return buildTopic("appointment-events", properties, null);
    }

    @Bean
    public NewTopic notificationEventsTopic(PlatformKafkaTopicsProperties properties) {
        return TopicBuilder.name("notification-events")
                .partitions(Math.max(2, properties.getDefaultPartitions() - 1))
                .replicas(properties.getDefaultReplicas())
                .build();
    }

    private static NewTopic buildTopic(String name, PlatformKafkaTopicsProperties properties, Long retentionMs) {
        TopicBuilder builder = TopicBuilder.name(name)
                .partitions(properties.getDefaultPartitions())
                .replicas(properties.getDefaultReplicas());
        if (retentionMs != null) {
            builder.config("retention.ms", String.valueOf(retentionMs));
        }
        return builder.build();
    }
}
