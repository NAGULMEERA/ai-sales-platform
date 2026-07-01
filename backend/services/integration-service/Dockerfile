FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/integration-service-*.jar app.jar
EXPOSE 8092
ENTRYPOINT ["java", "-jar", "app.jar"]
