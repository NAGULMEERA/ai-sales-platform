package com.aisales.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aisales.identity")
@EnableDiscoveryClient
@EntityScan(basePackages = {
        "com.aisales.identity.domain.entity",
        "com.aisales.common.events.inbox"
})
@EnableJpaRepositories(basePackages = {
        "com.aisales.identity.infrastructure.persistence",
        "com.aisales.common.events.inbox"
})
public class IdentityServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityServiceApplication.class, args);
    }
}
