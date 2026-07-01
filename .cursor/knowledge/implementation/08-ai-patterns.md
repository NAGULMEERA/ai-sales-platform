# 08-ai-patterns.md

# AI Implementation Patterns

Version: 1.0

## Purpose

Defines production-ready AI implementation patterns for the AI Sales
Employee Platform using Spring AI/LangChain4j while keeping business
rules inside the domain.

------------------------------------------------------------------------

# AI Architecture Principles

-   AI assists, domain decides
-   LLMs are external dependencies
-   Prompts are versioned
-   Structured output is mandatory
-   Every AI call is observable

------------------------------------------------------------------------

# AI Request Flow

``` text
User Request
     │
     ▼
Application Service
     │
     ▼
AI Service
     │
 ┌───┴─────────────┐
 ▼                 ▼
RAG            Tool Calling
     │
     ▼
LLM
     │
     ▼
Structured Output
     │
     ▼
Validation
     │
     ▼
Domain
```

------------------------------------------------------------------------

# Prompt Pattern

Store prompts outside Java code.

Each prompt has

-   Prompt ID
-   Version
-   Variables
-   Output schema
-   Owner

Never hardcode production prompts.

------------------------------------------------------------------------

# Structured Output

Prefer JSON/object schemas.

Validate

-   Required fields
-   Enums
-   Numeric ranges
-   Business constraints

Never trust raw LLM output.

------------------------------------------------------------------------

# Tool Calling

Tools should be

-   Small
-   Idempotent
-   Authenticated
-   Audited

Examples

-   SearchProperty
-   ScheduleAppointment
-   SendWhatsApp
-   CalculateEMI

------------------------------------------------------------------------

# Memory Pattern

Use

-   Conversation Memory
-   Customer Memory
-   Tenant Knowledge

Memory must remain tenant isolated.

------------------------------------------------------------------------

# RAG Pattern

Pipeline

``` text
Question
   │
Embedding
   │
Vector Search
   │
Relevant Documents
   │
LLM
```

Only trusted knowledge sources participate.

------------------------------------------------------------------------

# Guardrails

Validate

-   Prompt injection
-   PII leakage
-   Hallucinations
-   Unauthorized tool usage

Reject unsafe responses.

------------------------------------------------------------------------

# Multi-Model Strategy

Support

-   OpenAI
-   Gemini
-   Azure OpenAI

Choose model by capability, latency and cost.

------------------------------------------------------------------------

# Retry & Fallback

Retry

-   Network failures
-   Rate limits

Fallback

-   Secondary model
-   Cached response
-   Human escalation

------------------------------------------------------------------------

# Observability

Capture

-   Prompt version
-   Model
-   Tokens
-   Latency
-   Cost
-   Correlation ID

------------------------------------------------------------------------

# Review Checklist

-   Prompt versioned
-   Structured output validated
-   Guardrails enabled
-   Memory isolated
-   Tool access controlled
-   Metrics captured

------------------------------------------------------------------------

# Related Knowledge

-   07-ai-architecture.md
-   08-ai-domain.md
-   08-security-standards.md
-   09-observability-standards.md

# End
