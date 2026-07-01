package com.aisales.infrastructure.discovery;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@SpringBootApplication
@EnableDiscoveryClient
public class DiscoveryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DiscoveryServiceApplication.class, args);
    }

    @RestController
    static class DiscoveryController {
        @GetMapping("/api/v1/discovery/status")
        Map<String, String> status() {
            return Map.of("status", "UP", "service", "discovery-service");
        }
    }
}
