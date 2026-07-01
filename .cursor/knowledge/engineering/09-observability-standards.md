# 09-observability-standards.md

# Observability Engineering Standards

Version: 1.0

## Purpose

Defines observability standards to ensure every service is measurable,
debuggable, and production-ready.

------------------------------------------------------------------------

# Observability Principles

-   Build observability from day one
-   Prefer structured telemetry
-   Monitor technical and business metrics
-   Every request must be traceable

------------------------------------------------------------------------

# Three Pillars

-   Logs
-   Metrics
-   Traces

All production services must implement all three.

------------------------------------------------------------------------

# Logging Standards

Use structured JSON logs.

Include

-   Timestamp
-   Correlation ID
-   Tenant ID
-   User ID
-   Service Name
-   Log Level

Never log

-   Passwords
-   API Keys
-   Secrets
-   Sensitive PII

------------------------------------------------------------------------

# Metrics Standards

Technical metrics

-   Request Count
-   Error Rate
-   Latency
-   JVM Memory
-   CPU
-   Database Connections

Business metrics

-   Leads Created
-   Appointments Scheduled
-   AI Success Rate
-   Conversion Rate
-   Revenue

Export metrics using Micrometer.

------------------------------------------------------------------------

# Distributed Tracing

Use OpenTelemetry.

Trace

-   HTTP Requests
-   Kafka Messages
-   Workflow Execution
-   AI Calls
-   Plugin Calls
-   Database Queries

Every trace carries Correlation ID and Trace ID.

------------------------------------------------------------------------

# Health Checks

Expose

-   Liveness
-   Readiness
-   Startup

Validate

-   PostgreSQL
-   Redis
-   Kafka
-   AI Providers
-   External Integrations

------------------------------------------------------------------------

# Alerting

Create alerts for

-   High Error Rate
-   High Latency
-   Kafka Lag
-   Workflow Failures
-   AI Failures
-   Database Connectivity

Alerts must have runbooks.

------------------------------------------------------------------------

# Dashboards

Maintain dashboards for

-   Platform Health
-   API Performance
-   Business KPIs
-   AI Usage
-   Workflow Health
-   Infrastructure

------------------------------------------------------------------------

# SLI / SLO

Track

-   Availability
-   P95 Latency
-   Error Rate
-   Workflow Success
-   AI Response Time

Define measurable SLOs for every critical service.

------------------------------------------------------------------------

# Engineering Checklist

-   Structured logging enabled
-   Metrics exported
-   Tracing enabled
-   Health checks implemented
-   Dashboards updated
-   Alerts configured
-   Runbooks documented

------------------------------------------------------------------------

# Related Knowledge

-   09-observability-architecture.md
-   06-event-standards.md
-   02-spring-boot-standards.md

# End
