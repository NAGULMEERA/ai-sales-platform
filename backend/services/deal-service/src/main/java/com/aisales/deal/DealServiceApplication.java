package com.aisales.deal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aisales.deal")
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.aisales.deal",
        "com.aisales.common.events.outbox"
})
@EnableJpaRepositories(basePackages = {
        "com.aisales.deal",
        "com.aisales.common.events.outbox"
})
public class DealServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DealServiceApplication.class, args);
    }
}
