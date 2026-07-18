package com.aisales.customer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aisales.customer")
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.aisales.customer",
        "com.aisales.common.events.outbox"
})
@EnableJpaRepositories(basePackages = {
        "com.aisales.customer",
        "com.aisales.common.events.outbox"
})
public class CustomerServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CustomerServiceApplication.class, args);
    }
}
