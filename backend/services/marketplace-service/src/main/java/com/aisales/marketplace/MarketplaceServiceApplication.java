package com.aisales.marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aisales.marketplace")
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.aisales.marketplace",
        "com.aisales.common.events.outbox"
})
@EnableJpaRepositories(basePackages = {
        "com.aisales.marketplace",
        "com.aisales.common.events.outbox"
})
public class MarketplaceServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MarketplaceServiceApplication.class, args);
    }
}
