package com.aisales.common.events.model;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Immutable integration/domain event base after construction.
 * Factories assign fields once; Jackson deserializes via fields (no setters).
 */
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
        isGetterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
        setterVisibility = JsonAutoDetect.Visibility.NONE)
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
}
