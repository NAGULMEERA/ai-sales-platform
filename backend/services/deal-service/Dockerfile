FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/deal-service-*.jar app.jar
EXPOSE 8097
ENTRYPOINT ["java", "-jar", "app.jar"]