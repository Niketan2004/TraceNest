# ==============================================================================
# TraceNest Backend Dockerfile
# Multi-stage build with Eclipse Temurin JDK 25 & JRE 25
# ==============================================================================

# ------------------------------------------------------------------------------
# 1. Build Stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jdk AS builder
WORKDIR /app

# Install curl and ca-certificates for Maven wrapper downloads
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl ca-certificates && \
    rm -rf /var/lib/apt/lists/*

# Copy wrapper files and pom.xml first for layer caching
COPY .mvn .mvn
COPY mvnw pom.xml ./

# Make wrapper script executable and pre-fetch project dependencies
RUN sed -i 's/\r$//' ./mvnw && chmod +x ./mvnw && ./mvnw dependency:go-offline -B || true

# Copy project source code
COPY src ./src

# Build production Spring Boot executable JAR without running tests
RUN ./mvnw clean package -DskipTests -B

# ------------------------------------------------------------------------------
# 2. Runtime Stage
# ------------------------------------------------------------------------------
FROM eclipse-temurin:25-jre AS runner
WORKDIR /app

# Install curl for container health checks
RUN apt-get update && \
    apt-get install -y --no-install-recommends curl && \
    rm -rf /var/lib/apt/lists/*

# Create unprivileged system group and user
RUN groupadd -r tracenest && useradd -r -g tracenest -s /bin/false tracenest

# Copy compiled JAR from builder stage
COPY --from=builder /app/target/*.jar app.jar

# Set correct ownership
RUN chown -R tracenest:tracenest /app

# Switch to non-root user
USER tracenest

# Backend HTTP port
EXPOSE 8080

# Environment defaults
ENV JAVA_OPTS="-XX:+EnableDynamicAgentLoading -Dspring.classformat.ignore=true -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
