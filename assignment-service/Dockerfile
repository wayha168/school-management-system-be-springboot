# Runtime-only image (avoids pulling maven from Docker Hub).
FROM eclipse-temurin:25-jre
WORKDIR /app
COPY target/assignment-service-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
