package com.aisales.common.events.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {

    private String eventId;
    private String eventType;
    private String tenantId;
    private String aggregateId;
    private Instant occurredAt;
    private String correlationId;
    @JsonProperty("eventVersion")
    @JsonAlias("version")
    private int eventVersion;

    protected void init(String eventType, String tenantId, String aggregateId, String correlationId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.tenantId = tenantId;
        this.aggregateId = aggregateId;
        this.occurredAt = Instant.now();
        this.correlationId = correlationId;
        this.eventVersion = 1;
    }

    @Deprecated(forRemoval = true)
    @JsonIgnore
    public int getVersion() {
        return eventVersion;
    }

    @Deprecated(forRemoval = true)
    @JsonIgnore
    public void setVersion(int version) {
        this.eventVersion = version;
    }
}
