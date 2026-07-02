package com.aisales.common.events.integration;

import com.aisales.common.events.inbox.InboxService;
import com.aisales.common.events.model.TenantCreatedEvent;
import com.aisales.common.events.outbox.OutboxDispatchScheduler;
import com.aisales.common.events.outbox.OutboxEvent;
import com.aisales.common.events.outbox.OutboxEventPublisher;
import com.aisales.common.events.outbox.OutboxRepository;
import com.aisales.common.testing.containers.PlatformTestcontainers;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end: transactional outbox publish → Kafka dispatch → idempotent inbox consumer.
 */
@SpringBootTest(classes = EventingIntegrationTestApplication.class)
@Testcontainers(disabledWithoutDocker = true)
class OutboxToInboxIntegrationIT {

    private static final String TOPIC = "aisales-events-test";

    @Container
    static PostgreSQLContainer<?> postgres = PlatformTestcontainers.postgres("eventing_test");

    @Container
    static KafkaContainer kafka = PlatformTestcontainers.kafka();

    @Autowired
    private OutboxEventPublisher outboxEventPublisher;

    @Autowired
    private OutboxDispatchScheduler outboxDispatchScheduler;

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private InboxService inboxService;

    @Autowired
    private TestTenantCreatedConsumer testConsumer;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("aisales.events.default-topic", () -> TOPIC);
    }

    @BeforeEach
    void setUp() throws Exception {
        createTopicIfMissing();
        testConsumer.reset();
    }

    @Test
    void shouldPublishViaOutboxAndConsumeViaInbox() throws Exception {
        TenantCreatedEvent event = TenantCreatedEvent.of(
                "tenant-e2e-1", "Acme Corp", "acme", "FREE", "REAL_ESTATE", "corr-e2e-1");

        transactionTemplate.executeWithoutResult(status -> outboxEventPublisher.publish(event));

        outboxDispatchScheduler.dispatchPendingEvents();

        assertThat(testConsumer.awaitEvent(30, TimeUnit.SECONDS)).isTrue();

        assertThat(outboxRepository.findAll())
                .isNotEmpty()
                .allMatch(e -> e.getStatus() == OutboxEvent.OutboxStatus.PUBLISHED);

        assertThat(testConsumer.getLastEvent().get()).isNotNull();
        assertThat(testConsumer.getLastEvent().get().getEventId()).isEqualTo(event.getEventId());
        assertThat(inboxService.isProcessed(event.getEventId(), TestTenantCreatedConsumer.CONSUMER_NAME)).isTrue();
    }

    private void createTopicIfMissing() throws Exception {
        Properties props = new Properties();
        props.put("bootstrap.servers", kafka.getBootstrapServers());
        try (AdminClient admin = AdminClient.create(props)) {
            admin.createTopics(List.of(new NewTopic(TOPIC, 1, (short) 1))).all().get();
        } catch (Exception ex) {
            // Topic may already exist on retry.
        }
    }
}
