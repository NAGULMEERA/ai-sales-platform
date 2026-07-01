package com.company.platform.template.domain.aggregate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Production Aggregate Root Template
 */
public abstract class AggregateRoot<ID> {

    private final ID id;
    private final List<Object> domainEvents = new ArrayList<>();

    protected AggregateRoot(ID id) {
        this.id = id;
    }

    public ID getId() {
        return id;
    }

    protected void raiseEvent(Object event) {
        domainEvents.add(event);
    }

    public List<Object> domainEvents() {
        return List.copyOf(domainEvents);
    }

    public void clearDomainEvents() {
        domainEvents.clear();
    }
}

// Example aggregate

class LeadAggregate extends AggregateRoot<UUID> {

    private String status;

    public LeadAggregate(UUID id) {
        super(id);
        this.status = "CAPTURED";
        raiseEvent(new LeadCreated(id));
    }

    public void qualify() {
        if (!"CAPTURED".equals(status)) {
            throw new IllegalStateException("Only captured leads can be qualified.");
        }
        status = "QUALIFIED";
        raiseEvent(new LeadQualified(getId()));
    }
}

record LeadCreated(UUID leadId) {}
record LeadQualified(UUID leadId) {}
