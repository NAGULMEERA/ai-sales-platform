package com.aisales.lead;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = "com.aisales.lead")
@EnableDiscoveryClient
public class LeadServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeadServiceApplication.class, args);
    }
}
