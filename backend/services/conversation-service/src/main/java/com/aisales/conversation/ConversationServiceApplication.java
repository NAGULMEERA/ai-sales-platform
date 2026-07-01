package com.aisales.conversation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.aisales.conversation")
@EnableDiscoveryClient
public class ConversationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ConversationServiceApplication.class, args);
    }
}
