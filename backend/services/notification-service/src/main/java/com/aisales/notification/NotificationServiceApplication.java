package com.aisales.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aisales.notification")
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.aisales.notification",
        "com.aisales.common.events.inbox"
})
@EnableJpaRepositories(basePackages = {
        "com.aisales.notification",
        "com.aisales.common.events.inbox"
})
public class NotificationServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
