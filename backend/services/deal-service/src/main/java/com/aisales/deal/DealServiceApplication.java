package com.aisales.deal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.aisales.deal")
@EnableDiscoveryClient
public class DealServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DealServiceApplication.class, args);
    }
}
