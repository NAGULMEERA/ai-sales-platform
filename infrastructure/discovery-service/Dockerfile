FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/discovery-service-*.jar app.jar
EXPOSE 8762
ENTRYPOINT ["java", "-jar", "app.jar"]
