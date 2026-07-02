package com.aisales.common.events.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "aisales.events.kafka")
public class PlatformKafkaTopicsProperties {

    /** When true, register standard platform Kafka topics at startup. */
    private boolean autoCreateTopics = true;

    private int defaultPartitions = 3;

    private int defaultReplicas = 1;
}
