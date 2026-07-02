package com.aisales.common.events.audit;

import com.aisales.common.core.audit.AuditRecord;
import com.aisales.common.events.publisher.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventPublishingAuditRecorderTest {

    @Mock
    private EventPublisher eventPublisher;

    private EventPublishingAuditRecorder recorder;

    @Test
    void shouldPublishAuditRecordedEvent() {
        UUID tenantId = UUID.randomUUID();
        recorder = new EventPublishingAuditRecorder(eventPublisher, "aisales-events");
        AuditRecord record = AuditRecord.builder()
                .tenantId(tenantId)
                .userId("user-1")
                .action("UPDATE")
                .resourceType("lead")
                .resourceId("lead-1")
                .correlationId("corr-1")
                .build();

        recorder.record(record);

        ArgumentCaptor<AuditRecordedEvent> captor = ArgumentCaptor.forClass(AuditRecordedEvent.class);
        verify(eventPublisher).publish(org.mockito.ArgumentMatchers.eq("aisales-events"), captor.capture());
        AuditRecordedEvent event = captor.getValue();
        assertThat(event.getEventType()).isEqualTo(AuditRecordedEvent.EVENT_TYPE);
        assertThat(event.getTenantId()).isEqualTo(tenantId.toString());
        assertThat(event.getResourceId()).isEqualTo("lead-1");
        assertThat(event.getAction()).isEqualTo("UPDATE");
    }
}
