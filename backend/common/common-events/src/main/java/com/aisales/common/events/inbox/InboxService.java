package com.aisales.common.events.inbox;

import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Insert-before-handler claim. Must run inside the handler transaction.
     * Returns false when another consumer already claimed/processed the event.
     * On handler failure the surrounding TX rolls back the claim.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean tryClaim(String eventId, String consumerName) {
        if (eventId == null || eventId.isBlank()) {
            return false;
        }
        int inserted = processedEventRepository.insertIgnoreConflict(
                eventId, consumerName, Instant.now());
        return inserted > 0;
    }

    /**
     * @deprecated Prefer {@link #tryClaim(String, String)} at the start of the handler TX.
     */
    @Deprecated
    @Transactional(propagation = Propagation.MANDATORY)
    public void markProcessed(String eventId, String consumerName) {
        tryClaim(eventId, consumerName);
    }
}
