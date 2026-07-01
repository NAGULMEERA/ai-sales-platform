FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/workflow-service-*.jar app.jar
EXPOSE 8089
ENTRYPOINT ["java", "-jar", "app.jar"]
