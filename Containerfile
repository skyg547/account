# Multi-stage build for Spring Boot 3.4 / Java 21 Microservices
# Build Stage
FROM eclipse-temurin:21-jdk-alpine AS builder

WORKDIR /app
COPY . .

# Argument to pass which module to build (e.g., closing/api, journal-ledger/batch)
ARG MODULE_NAME
# Build only the required module
RUN ./gradlew :${MODULE_NAME}:bootJar --no-daemon

# Find the built jar and rename it to app.jar
RUN find ${MODULE_NAME}/build/libs/ -name "*.jar" -not -name "*plain.jar" -exec cp {} app.jar \;

# Run Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy built jar from builder
COPY --from=builder /app/app.jar /app/app.jar

# JVM options optimized for containers
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -XX:+UseG1GC -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/app.jar"]
