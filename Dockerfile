# ==============================================================================
# Multi-Stage Production Dockerfile for SalesCRM Spring WebFlux Application
# ==============================================================================

# Stage 1: Build application JAR
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /workspace

# Copy Maven configuration and download dependencies
COPY pom.xml .
RUN mvn dependency:go-offline -B -DskipTests || true

# Copy source code and build
COPY src ./src
COPY checkstyle.xml .

RUN mvn clean package -DskipTests -Pprod

# Stage 2: Production runtime
FROM eclipse-temurin:21-jre-noble

LABEL maintainer="SalesCRM Team"
LABEL description="SalesCRM Reactive Spring WebFlux Application"

# Install curl for health checks
RUN apt-get update && \
  apt-get install -y --no-install-recommends curl && \
  rm -rf /var/lib/apt/lists/*

# Create non-root user
RUN groupadd -r crmuser && \
  useradd -r -g crmuser -s /usr/sbin/nologin crmuser

WORKDIR /app

# Copy executable JAR
COPY --from=builder /workspace/target/*.jar /app/salescrm.jar

# Set permissions
RUN chown crmuser:crmuser /app/salescrm.jar

USER crmuser

# Spring configuration
ENV SPRING_PROFILES_ACTIVE=prod
ENV SERVER_PORT=8080

EXPOSE 8080

# Container health check
HEALTHCHECK --interval=30s --timeout=5s --start-period=60s --retries=3 \
  CMD curl -fsS http://localhost:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "/app/salescrm.jar"]