# Stage 1: Build
FROM docker.io/library/gradle:8.7-jdk17-alpine AS builder
USER root
RUN mkdir -p /workspace && chown gradle:gradle /workspace
USER gradle
WORKDIR /workspace

COPY --chown=gradle:gradle . .

RUN ./gradlew :journal-ledger:batch:bootJar --console=plain --no-daemon

# Stage 2: Runtime
FROM docker.io/library/eclipse-temurin:17-jre-alpine
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app
COPY --from=builder --chown=app:app /workspace/journal-ledger/batch/build/libs/*-SNAPSHOT.jar /app/app.jar

USER app:app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
