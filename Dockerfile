# Use OpenJDK 21 as the base image for building
FROM eclipse-temurin:21

# Set the working directory
WORKDIR /app

# Copy the Maven wrapper and configuration files first
COPY target/sales-crm-0.0.1-SNAPSHOT.jar /app/sales-crm-0.0.1-SNAPSHOT.jar

## Ensure the Maven wrapper is executable
#RUN chmod +x /app/mvnw

## Copy the rest of the project files
#COPY pom.xml /app/pom.xml
#COPY src /app/src
#
## Debug: Check if mvnw exists and is executable
#RUN ls -la /app

## Build the JHipster application (skipping tests)
#RUN /app/mvnw package -DskipTests
#
## Use a minimal OpenJDK 21 runtime for the final image
#FROM openjdk:21-jdk-slim
#
## Set the working directory for the runtime container
#WORKDIR /app
#
## Copy the built JAR file from the build stage
#COPY --from=build /app/target/*.jar app.jar

# Expose the application port (default 8080)
EXPOSE 8080

# Command to run the application
ENTRYPOINT ["java", "-jar", "sales-crm-0.0.1-SNAPSHOT.jar"]
