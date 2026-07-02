package com.aisales.common.events.integration;

import com.aisales.common.events.consumer.IntegrationEventListener;
import com.aisales.common.events.model.TenantCreatedEvent;
import lombok.Getter;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Component
@Getter
public class TestTenantCreatedConsumer {

    static final String CONSUMER_NAME = "eventing-integration-test";

    private final IntegrationEventListener integrationEventListener;
    private final AtomicReference<TenantCreatedEvent> lastEvent = new AtomicReference<>();
    private volatile CountDownLatch latch = new CountDownLatch(1);

    public TestTenantCreatedConsumer(IntegrationEventListener integrationEventListener) {
        this.integrationEventListener = integrationEventListener;
    }

    @KafkaListener(
            topics = "${aisales.events.default-topic}",
            groupId = "eventing-integration-test",
            containerFactory = "integrationKafkaListenerContainerFactory")
    public void onMessage(ConsumerRecord<String, String> record) {
        integrationEventListener.handle(record, CONSUMER_NAME, TenantCreatedEvent.class, event -> {
            lastEvent.set(event);
            latch.countDown();
        });
    }

    public boolean awaitEvent(long timeout, TimeUnit unit) throws InterruptedException {
        return latch.await(timeout, unit);
    }

    public void reset() {
        lastEvent.set(null);
        latch = new CountDownLatch(1);
    }
}
