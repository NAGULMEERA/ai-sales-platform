package com.aisales.common.events.inbox;

import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class InboxService {

    private final ProcessedEventRepository processedEventRepository;

    @Transactional(readOnly = true)
    public boolean isProcessed(String eventId, String consumerName) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        return processedEventRepository.existsByEventIdAndConsumerName(eventId, consumerName);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void markProcessed(String eventId, String consumerName) {
        if (eventId == null || eventId.isBlank()) {
            return;
        }
        if (processedEventRepository.existsByEventIdAndConsumerName(eventId, consumerName)) {
            return;
        }
        try {
            processedEventRepository.save(ProcessedEvent.builder()
                    .eventId(eventId)
                    .consumerName(consumerName)
                    .processedAt(Instant.now())
                    .build());
        } catch (DataIntegrityViolationException ex) {
            // Concurrent consumer already recorded this event.
        }
    }
}
