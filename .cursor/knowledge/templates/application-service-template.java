package com.company.platform.template.application;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Production-ready Application Service Template.
 *
 * Responsibilities:
 * - Orchestrates a use case
 * - Starts/ends transaction
 * - Loads aggregate
 * - Invokes domain behavior
 * - Persists aggregate
 * - Publishes domain events
 */
@Service
public class LeadApplicationService {

    private final LeadRepository repository;
    private final DomainEventPublisher eventPublisher;
    private final TenantContext tenantContext;

    public LeadApplicationService(
            LeadRepository repository,
            DomainEventPublisher eventPublisher,
            TenantContext tenantContext) {
        this.repository = repository;
        this.eventPublisher = eventPublisher;
        this.tenantContext = tenantContext;
    }

    @Transactional
    public UUID qualifyLead(QualifyLeadCommand command) {

        LeadAggregate lead = repository.findById(command.leadId())
                .orElseThrow(() -> new LeadNotFoundException(command.leadId()));

        lead.qualify(command.score());

        repository.save(lead);

        lead.domainEvents().forEach(eventPublisher::publish);
        lead.clearDomainEvents();

        return lead.getId();
    }
}

/* ---------- Command ---------- */

record QualifyLeadCommand(
        UUID leadId,
        int score) {}

/* ---------- Ports ---------- */

interface LeadRepository {
    java.util.Optional<LeadAggregate> findById(UUID id);
    LeadAggregate save(LeadAggregate aggregate);
}

interface DomainEventPublisher {
    void publish(Object event);
}

interface TenantContext {
    UUID tenantId();
}

/* ---------- Domain ---------- */

class LeadAggregate {

    private final UUID id = UUID.randomUUID();

    UUID getId() {
        return id;
    }

    void qualify(int score) {
        // Business logic belongs here.
    }

    java.util.List<Object> domainEvents() {
        return java.util.List.of();
    }

    void clearDomainEvents() {}
}

/* ---------- Exception ---------- */

class LeadNotFoundException extends RuntimeException {
    LeadNotFoundException(UUID id) {
        super("Lead not found: " + id);
    }
}
