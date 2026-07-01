# ADR-001: Java 21 LTS

## Status

Accepted (supersedes Java 17 baseline)

## Context

The platform originally standardized on Java 17 LTS. Java 21 is the current long-term support release and is fully supported by Spring Boot 3.2.x.

## Decision

Use **Java 21 LTS** for all backend services, CI pipelines, and Docker runtime images.

## Consequences

- Developers install JDK 21 locally (IntelliJ project SDK, `JAVA_HOME`).
- Maven compiles with `java.version=21`.
- GitHub Actions and Docker images use Eclipse Temurin 21.
- Java 17-specific workarounds are no longer required; prefer Java 21 language and API features only when they improve clarity.

## Migration from Java 17

1. Install JDK 21.
2. Set IntelliJ **Project SDK** and Maven **JRE** to 21.
3. Run `mvn clean verify` from `backend/`.
