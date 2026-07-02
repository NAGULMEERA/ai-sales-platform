package com.aisales.common.events.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.aisales.common.events")
public class EventingIntegrationTestApplication {

    public static void main(String[] args) {
        SpringApplication.run(EventingIntegrationTestApplication.class, args);
    }
}
