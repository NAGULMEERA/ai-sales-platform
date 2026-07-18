package com.aisales.common.events.inbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InboxServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private InboxService inboxService;

    @Test
    void shouldClaimWhenInsertSucceeds() {
        when(processedEventRepository.insertIgnoreConflict(eq("evt-1"), eq("tenant-service"), any(Instant.class)))
                .thenReturn(1);

        assertThat(inboxService.tryClaim("evt-1", "tenant-service")).isTrue();
    }

    @Test
    void shouldNotClaimWhenConflict() {
        when(processedEventRepository.insertIgnoreConflict(eq("evt-1"), eq("tenant-service"), any(Instant.class)))
                .thenReturn(0);

        assertThat(inboxService.tryClaim("evt-1", "tenant-service")).isFalse();
    }

    @Test
    void shouldDetectAlreadyProcessed() {
        when(processedEventRepository.existsByEventIdAndConsumerName("evt-1", "tenant-service")).thenReturn(true);

        assertThat(inboxService.isProcessed("evt-1", "tenant-service")).isTrue();
        verify(processedEventRepository, never()).insertIgnoreConflict(any(), any(), any());
    }
}
