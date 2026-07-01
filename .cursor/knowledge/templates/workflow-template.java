package com.company.platform.template.workflow;

import java.time.Instant;
import java.util.UUID;

/**
 * Production-ready Workflow Template.
 *
 * Workflow coordinates long-running business processes.
 * Business rules remain inside aggregates/application services.
 */
public class LeadQualificationWorkflow {

    private final WorkflowRepository repository;
    private final DomainEventPublisher publisher;

    public LeadQualificationWorkflow(
            WorkflowRepository repository,
            DomainEventPublisher publisher) {
        this.repository = repository;
        this.publisher = publisher;
    }

    public void start(UUID workflowId, UUID leadId) {

        WorkflowInstance workflow =
                WorkflowInstance.start(workflowId, leadId);

        repository.save(workflow);

        publisher.publish(new WorkflowStarted(workflowId, leadId));
    }

    public void approve(UUID workflowId) {

        WorkflowInstance workflow =
                repository.findById(workflowId);

        workflow.transitionTo(State.APPROVED);

        repository.save(workflow);

        publisher.publish(new WorkflowApproved(workflowId));
    }

    public void compensate(UUID workflowId) {

        WorkflowInstance workflow =
                repository.findById(workflowId);

        workflow.transitionTo(State.COMPENSATING);

        // Execute compensation actions here
        // e.g. cancel appointment, refund payment, release reservation

        workflow.transitionTo(State.COMPENSATED);

        repository.save(workflow);

        publisher.publish(new WorkflowCompensated(workflowId));
    }
}

/* ---------- Workflow Aggregate ---------- */

class WorkflowInstance {

    private final UUID id;
    private final UUID leadId;
    private State state;
    private final Instant createdAt;

    private WorkflowInstance(UUID id, UUID leadId) {
        this.id = id;
        this.leadId = leadId;
        this.state = State.CREATED;
        this.createdAt = Instant.now();
    }

    static WorkflowInstance start(UUID id, UUID leadId) {
        WorkflowInstance wf = new WorkflowInstance(id, leadId);
        wf.transitionTo(State.RUNNING);
        return wf;
    }

    void transitionTo(State next) {

        if (!state.canTransitionTo(next)) {
            throw new IllegalStateException(
                    "Invalid transition: " + state + " -> " + next);
        }

        this.state = next;
    }

    UUID getId() {
        return id;
    }
}

/* ---------- States ---------- */

enum State {
    CREATED,
    RUNNING,
    WAITING,
    APPROVED,
    COMPLETED,
    COMPENSATING,
    COMPENSATED,
    FAILED;

    boolean canTransitionTo(State next) {
        return switch (this) {
            case CREATED -> next == RUNNING;
            case RUNNING -> next == WAITING || next == APPROVED || next == FAILED;
            case WAITING -> next == APPROVED || next == FAILED;
            case APPROVED -> next == COMPLETED || next == COMPENSATING;
            case COMPENSATING -> next == COMPENSATED;
            default -> true;
        };
    }
}

/* ---------- Ports ---------- */

interface WorkflowRepository {
    WorkflowInstance findById(UUID id);
    void save(WorkflowInstance workflow);
}

interface DomainEventPublisher {
    void publish(Object event);
}

/* ---------- Events ---------- */

record WorkflowStarted(UUID workflowId, UUID leadId) {}
record WorkflowApproved(UUID workflowId) {}
record WorkflowCompensated(UUID workflowId) {}
