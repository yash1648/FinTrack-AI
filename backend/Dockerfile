# =============================================================================
# FinTrack AI — Backend Dockerfile (Multi-stage, Production)
# =============================================================================

# ── Build Stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Install curl for healthcheck
RUN apk add --no-cache curl

# Copy Maven wrapper and project files
COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# Make mvnw executable
RUN chmod +x mvnw

# Download dependencies (layer is cached unless pom.xml changes)
RUN ./mvnw dependency:go-offline -B -q

# Copy source and build
COPY src src

# Build with optimizations
RUN ./mvnw package \
    -DskipTests \
    -Dcheckstyle.skip \
    -q \
    --no-transfer-progress

# Extract layers for efficient Docker caching (Spring Boot layered JAR)
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# ── Runtime Stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Add non-root user
RUN addgroup -S fintrack && \
    adduser -S -G fintrack fintrack && \
    apk add --no-cache curl tzdata

WORKDIR /app

# Create logs directory
RUN mkdir -p /app/logs && chown -R fintrack:fintrack /app/logs

# Copy extracted layers from build stage (optimized layer caching)
COPY --from=build --chown=fintrack:fintrack /app/extracted/dependencies/ ./
COPY --from=build --chown=fintrack:fintrack /app/extracted/spring-boot-loader/ ./
COPY --from=build --chown=fintrack:fintrack /app/extracted/snapshot-dependencies/ ./
COPY --from=build --chown=fintrack:fintrack /app/extracted/application/ ./

# Expose application port
EXPOSE 8080

# Switch to non-root user
USER fintrack

# JVM optimized for containerized environments
ENTRYPOINT ["sh", "-c", "java \
    ${JAVA_OPTS:-} \
    -XX:+UseZGC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom \
    -Dnetworkaddress.cache.ttl=60 \
    -jar org.springframework.boot.loader.launch.JarLauncher"]

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=30s \
    CMD curl -sf http://localhost:8080/actuator/health || exit 1
