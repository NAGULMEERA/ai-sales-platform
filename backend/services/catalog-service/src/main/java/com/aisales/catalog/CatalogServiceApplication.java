package com.aisales.catalog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aisales.catalog")
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.aisales.catalog",
        "com.aisales.common.events.outbox"
})
@EnableJpaRepositories(basePackages = {
        "com.aisales.catalog",
        "com.aisales.common.events.outbox"
})
public class CatalogServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogServiceApplication.class, args);
    }
}
