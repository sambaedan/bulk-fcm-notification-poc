# Use Java 25 JDK
FROM eclipse-temurin:25-jdk-alpine

LABEL authors="lenovo"

WORKDIR /app

# Copy the Spring Boot jar (make sure it’s built with Java 21)
COPY target/*.jar app.jar

# Expose the port your Spring Boot app uses
EXPOSE 8080

# Run the jar
ENTRYPOINT ["java", "-jar", "app.jar"]
