# ==============================================================================
# Multi-Stage Production Dockerfile for SalesCRM Spring WebFlux Application
# Base: Eclipse Temurin OpenJDK 21
# ==============================================================================

# Stage 1: Build application jar
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /workspace

# Cache Maven dependencies layer
COPY pom.xml .
RUN mvn dependency:go-offline -B -DskipTests || true

# Copy source code and build
COPY src ./src
COPY checkstyle.xml .
RUN mvn clean package -DskipTests -Pprod

# Stage 2: Minimal, secure runtime container
FROM eclipse-temurin:21-jre-noble

LABEL maintainer="SalesCRM Team"
LABEL description="SalesCRM Reactive Spring WebFlux Application"

# Install curl for container healthcheck
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Run as non-privileged system user
RUN groupadd -r crmuser && useradd -r -g crmuser -s /bin/false crmuser

WORKDIR /app

# Copy executable jar from builder stage
COPY --from=builder /workspace/target/*.jar /app/salescrm.jar

# Set ownership
RUN chown -R crmuser:crmuser /app

USER crmuser

ENV SPRING_PROFILES_ACTIVE=prod \
    JAVA_OPTS="-Xms256m -Xmx1024m -XX:+UseG1GC -XX:+ExitOnOutOfMemoryError" \
    SERVER_PORT=8080

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=30s --retries=3 \
  CMD curl -f http://localhost:8080/management/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/salescrm.jar"]
