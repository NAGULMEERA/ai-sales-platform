package com.aisales.common.events.inbox;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, ProcessedEvent.ProcessedEventId> {

    boolean existsByEventIdAndConsumerName(String eventId, String consumerName);
}
