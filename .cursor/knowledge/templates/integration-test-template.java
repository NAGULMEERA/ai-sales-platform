package com.company.platform.template.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Production-ready Spring Boot Integration Test Template.
 */
@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class LeadIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:17");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    MockMvc mockMvc;

    @Test
    void shouldCreateLead() throws Exception {

        String body = """
        {
          "name":"John",
          "email":"john@test.com",
          "phone":"+919999999999"
        }
        """;

        mockMvc.perform(post("/api/v1/leads")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key","demo-key")
                .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.status").value("CAPTURED"));
    }
}

/*
Recommended additions:

- Flyway migrations enabled
- Kafka Testcontainer
- Redis Testcontainer
- Repository assertions
- Domain event verification
- Security integration tests
- Test data builders
- WireMock for external APIs
*/
