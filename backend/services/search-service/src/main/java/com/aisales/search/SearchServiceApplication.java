package com.aisales.search;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "com.aisales.search")
@EnableDiscoveryClient
// Outbox JPA wiring is owned by common-events OutboxAutoConfiguration
// (active when aisales.events.outbox.enabled=true). Do not re-declare it here.
@EntityScan(basePackages = {
        "com.aisales.search",
        "com.aisales.common.events.inbox"
})
@EnableJpaRepositories(basePackages = {
        "com.aisales.search",
        "com.aisales.common.events.inbox"
})
public class SearchServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SearchServiceApplication.class, args);
    }
}
