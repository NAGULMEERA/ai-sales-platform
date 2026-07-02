package com.aisales.common.events.inbox;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InboxServiceTest {

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @InjectMocks
    private InboxService inboxService;

    @Test
    void shouldSkipWhenAlreadyProcessed() {
        when(processedEventRepository.existsByEventIdAndConsumerName("evt-1", "tenant-service")).thenReturn(true);

        inboxService.markProcessed("evt-1", "tenant-service");

        verify(processedEventRepository, never()).save(any());
    }

    @Test
    void shouldIgnoreDuplicateKeyOnConcurrentMark() {
        when(processedEventRepository.existsByEventIdAndConsumerName("evt-1", "tenant-service")).thenReturn(false);
        when(processedEventRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate"));

        inboxService.markProcessed("evt-1", "tenant-service");

        verify(processedEventRepository).save(any());
    }
}
