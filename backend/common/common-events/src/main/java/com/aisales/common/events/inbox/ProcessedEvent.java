package com.aisales.common.events.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.Instant;

@Entity
@Table(name = "processed_events")
@IdClass(ProcessedEvent.ProcessedEventId.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {

    @Id
    @Column(name = "event_id", nullable = false)
    private String eventId;

    @Id
    @Column(name = "consumer_name", nullable = false, length = 100)
    private String consumerName;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class ProcessedEventId implements Serializable {
        private String eventId;
        private String consumerName;
    }
}
