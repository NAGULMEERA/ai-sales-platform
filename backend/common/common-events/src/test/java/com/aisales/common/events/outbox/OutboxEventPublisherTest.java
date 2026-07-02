package com.aisales.common.events.outbox;

import com.aisales.common.events.model.TenantCreatedEvent;
import com.aisales.common.events.publisher.EventPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OutboxEventPublisherTest {

    @Mock
    private OutboxRepository outboxRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @InjectMocks
    private OutboxEventPublisher outboxEventPublisher;

    @Test
    void shouldPersistEventInOutbox() {
        ReflectionTestUtils.setField(outboxEventPublisher, "defaultTopic", "aisales-events");
        TenantCreatedEvent event = TenantCreatedEvent.of(
                "tenant-1", "Acme", "acme", "FREE", "REAL_ESTATE", "corr-1");

        outboxEventPublisher.publish(event);

        ArgumentCaptor<OutboxEvent> captor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxRepository).save(captor.capture());
        OutboxEvent saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("TenantCreated");
        assertThat(saved.getAggregateId()).isEqualTo("tenant-1");
        assertThat(saved.getStatus()).isEqualTo(OutboxEvent.OutboxStatus.PENDING);
        assertThat(saved.getTopic()).isEqualTo("aisales-events");
    }
}
