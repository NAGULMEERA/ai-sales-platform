package com.aisales.deal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aisales.deal")
@EnableDiscoveryClient
// Outbox JPA wiring is owned by common-events OutboxAutoConfiguration
// (active when aisales.events.outbox.enabled=true). Do not re-declare it here.
@EntityScan(basePackages = "com.aisales.deal")
@EnableJpaRepositories(basePackages = "com.aisales.deal")
public class DealServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DealServiceApplication.class, args);
    }
}
