package com.aisales.common.core.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class LoggingAuditRecorderTest {

    private final LoggingAuditRecorder recorder = new LoggingAuditRecorder();

    @Test
    void shouldSkipWhenResourceIdMissing() {
        assertThatCode(() -> recorder.record(AuditRecord.builder()
                .action("CREATE")
                .resourceType("tenant")
                .build())).doesNotThrowAnyException();
    }

    @Test
    void shouldLogWhenResourceIdPresent() {
        assertThatCode(() -> recorder.record(AuditRecord.builder()
                .tenantId(UUID.randomUUID())
                .userId("user-1")
                .action("CREATE")
                .resourceType("tenant")
                .resourceId("abc")
                .correlationId("corr-1")
                .build())).doesNotThrowAnyException();
    }
}
