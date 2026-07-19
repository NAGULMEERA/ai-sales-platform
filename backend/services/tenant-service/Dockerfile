# syntax=docker/dockerfile:1.7
# Multi-stage image for tenant-service
# Build from repository root:
#   docker build -f services/tenant-service/Dockerfile -t aisales/tenant-service:latest .
# (For backend modules use: -f backend/services/tenant-service/Dockerfile with context = repo root)

FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace

RUN apk add --no-cache bash git

COPY backend/mvnw backend/pom.xml ./backend/
COPY backend/.mvn ./backend/.mvn
COPY backend/common ./backend/common
COPY backend/plugins ./backend/plugins
COPY backend/services ./backend/services
COPY infrastructure ./infrastructure

WORKDIR /workspace/backend
RUN chmod +x mvnw \
 && ./mvnw -pl services/tenant-service -am package -DskipTests -B \
 && cp services/tenant-service/target/tenant-service-*.jar /workspace/app.jar

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S aisales && adduser -S -G aisales aisales \
 && apk add --no-cache curl \
 && rm -rf /var/cache/apk/*

COPY --from=builder /workspace/app.jar /app/app.jar
RUN chown aisales:aisales /app/app.jar

USER aisales
EXPOSE 8082

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError" \
    SERVER_PORT=8082

HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS "http://127.0.0.1:8082/actuator/health/liveness" || exit 1

ENTRYPOINT ["java", "-jar", "/app/app.jar"]