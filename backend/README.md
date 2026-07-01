# Backend

Java 21 / Spring Boot 3.2 microservices monorepo.

## Layout

```
backend/
├── pom.xml           # Root Maven POM (includes ../infrastructure modules)
├── common/           # Shared libraries
├── services/         # Domain microservices
├── plugins/          # Plugin SDK and industry plugins
└── database/         # Lab Flyway migrations and service split maps
```

## Build

```bash
./mvnw clean install -DskipTests
./mvnw -pl services/identity-service -am test
```
