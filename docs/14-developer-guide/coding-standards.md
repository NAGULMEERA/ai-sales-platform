# Coding Standards

- Java 21, Spring Boot 3.2 conventions
- Package naming: `com.aisales.{service|common}.{module}`
- Use Lombok for boilerplate; MapStruct for entity-DTO mapping
- REST APIs under `/api/v1/`
- All responses wrapped in `ApiResponse<T>`
- Correlation ID propagated via `X-Correlation-Id` header
- Flyway for database migrations
- Spotless + Checkstyle enforced via Maven and pre-commit
