# =============================================================================
# FinTrack AI — Backend Dockerfile (Multi-stage, Production)
# Uses Spring Boot layered JAR for optimal Docker layer caching
# =============================================================================

# ── Build Stage ────────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jdk-alpine AS build

WORKDIR /app

# Install curl for healthcheck (and needed during build)
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

# Extract layers for efficient Docker caching
# (dependencies layer changes rarely, application layer changes often)
RUN java -Djarmode=layertools -jar target/*.jar extract --destination extracted

# ── Runtime Stage ──────────────────────────────────────────────────────────────
FROM eclipse-temurin:21-jre-alpine

# Add non-root user and install runtime tools
RUN addgroup -S fintrack && \
    adduser -S -G fintrack fintrack && \
    apk add --no-cache curl tzdata

WORKDIR /app

# Create logs directory
RUN mkdir -p /app/logs && chown -R fintrack:fintrack /app/logs

# ── Copy extracted layers (order matters for Docker caching) ──────────────────
# Layer 1: Dependencies (rarely changes)
COPY --from=build --chown=fintrack:fintrack /app/extracted/dependencies/ ./

# Layer 2: Spring Boot loader (rarely changes)
COPY --from=build --chown=fintrack:fintrack /app/extracted/spring-boot-loader/ ./

# Layer 3: Snapshot dependencies (changes occasionally)
COPY --from=build --chown=fintrack:fintrack /app/extracted/snapshot-dependencies/ ./

# Layer 4: Application code (changes most frequently — cached separately!)
COPY --from=build --chown=fintrack:fintrack /app/extracted/application/ ./

# Expose application port
EXPOSE 8080

# Switch to non-root user
USER fintrack

# JVM optimized for containerized environments
# Uses Spring Boot JarLauncher (from the extracted spring-boot-loader layer)
ENTRYPOINT ["sh", "-c", "java \
    ${JAVA_OPTS:-} \
    -XX:+UseZGC \
    -XX:MaxRAMPercentage=75.0 \
    -XX:+ExitOnOutOfMemoryError \
    -XX:+HeapDumpOnOutOfMemoryError \
    -XX:HeapDumpPath=/app/logs/heapdump.hprof \
    -Djava.security.egd=file:/dev/./urandom \
    -Dnetworkaddress.cache.ttl=60 \
    org.springframework.boot.loader.launch.JarLauncher"]

# Healthcheck
HEALTHCHECK --interval=30s --timeout=10s --retries=3 --start-period=30s \
    CMD curl -sf http://localhost:8080/actuator/health || exit 1
