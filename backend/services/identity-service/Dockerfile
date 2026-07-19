FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S aisales && adduser -S -G aisales aisales
COPY target/identity-service-*.jar app.jar
RUN chown aisales:aisales /app/app.jar

USER aisales
EXPOSE 8081

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"

ENTRYPOINT ["java", "-jar", "app.jar"]
