package com.aisales.conversation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aisales.conversation")
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.aisales.conversation",
        "com.aisales.common.events.outbox"
})
@EnableJpaRepositories(basePackages = {
        "com.aisales.conversation",
        "com.aisales.common.events.outbox"
})
public class ConversationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConversationServiceApplication.class, args);
    }
}
