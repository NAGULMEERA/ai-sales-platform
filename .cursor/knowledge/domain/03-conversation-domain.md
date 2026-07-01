# 03-conversation-domain.md

# Conversation Domain

Version: 1.0

## Purpose

Defines the Conversation bounded context using Domain-Driven Design
(DDD). This context manages customer conversations across all supported
channels while preserving history, context, and AI interactions.

------------------------------------------------------------------------

# Aggregate

**Conversation** is the aggregate root.

Responsibilities

-   Manage conversation lifecycle
-   Maintain session state
-   Preserve message history
-   Publish conversation events

------------------------------------------------------------------------

# Entities

-   Conversation (Aggregate Root)
-   Message
-   Participant
-   ConversationSession
-   Attachment
-   ConversationSummary

------------------------------------------------------------------------

# Value Objects

-   ConversationId
-   MessageId
-   Channel
-   Language
-   SentimentScore
-   Intent
-   Timestamp

Value objects are immutable.

------------------------------------------------------------------------

# Aggregate Invariants

A Conversation:

-   Must belong to one Tenant
-   Must contain at least one Participant
-   Cannot receive messages after closure unless reopened
-   Must preserve chronological message history
-   Must maintain channel context

------------------------------------------------------------------------

# Domain Services

-   ConversationRoutingService
-   ConversationSummaryService
-   IntentDetectionService
-   EscalationDecisionService

------------------------------------------------------------------------

# Repository

``` text
ConversationRepository
```

Responsibilities

-   Save Aggregate
-   Find by Id
-   Find by Tenant
-   Find Active Conversations
-   Search by Customer

------------------------------------------------------------------------

# Domain Events

-   ConversationStarted
-   MessageReceived
-   MessageSent
-   IntentDetected
-   EscalatedToHuman
-   ConversationClosed

Events are immutable.

------------------------------------------------------------------------

# State Model

``` text
Started
   │
Active
   │
Waiting
   │
Escalated
   │
Resolved
   │
Closed
```

------------------------------------------------------------------------

# Business Rules

-   Every message belongs to one conversation.
-   AI responses must be validated before delivery.
-   Escalation preserves complete context.
-   Conversation history cannot be lost.

------------------------------------------------------------------------

# Engineering Guidelines

Always

-   Preserve conversation history
-   Publish domain events
-   Use immutable value objects
-   Maintain tenant isolation
-   Audit escalations

Never

-   Delete conversation history
-   Modify closed conversations directly
-   Bypass AI validation
-   Share conversations across tenants

------------------------------------------------------------------------

# Related Knowledge

-   02-layered-architecture.md
-   03-bounded-contexts.md
-   04-conversation-lifecycle.md
-   05-event-driven-architecture.md
-   07-ai-architecture.md

# End
