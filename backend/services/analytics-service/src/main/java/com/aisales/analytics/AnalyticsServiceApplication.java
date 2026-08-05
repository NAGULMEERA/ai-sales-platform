package com.aisales.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aisales.analytics")
@EnableDiscoveryClient
// Outbox JPA wiring is owned by common-events OutboxAutoConfiguration
// (active when aisales.events.outbox.enabled=true). Do not re-declare it here.
@EntityScan(basePackages = {
        "com.aisales.analytics",
        "com.aisales.common.events.inbox"
})
@EnableJpaRepositories(basePackages = {
        "com.aisales.analytics",
        "com.aisales.common.events.inbox"
})
public class AnalyticsServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsServiceApplication.class, args);
    }
}
