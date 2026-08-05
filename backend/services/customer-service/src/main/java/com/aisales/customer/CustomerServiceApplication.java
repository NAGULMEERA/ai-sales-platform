package com.aisales.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aisales.customer")
@EnableDiscoveryClient
// Outbox JPA wiring is owned by common-events OutboxAutoConfiguration
// (active when aisales.events.outbox.enabled=true). Do not re-declare it here.
@EntityScan(basePackages = "com.aisales.customer")
@EnableJpaRepositories(basePackages = "com.aisales.customer")
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
