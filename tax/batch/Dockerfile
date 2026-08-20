FROM docker.io/library/gradle:8.7-jdk17-alpine AS builder
USER root
RUN mkdir -p /workspace && chown gradle:gradle /workspace
USER gradle
WORKDIR /workspace

COPY --chown=gradle:gradle . .

RUN set -eu; \
    ./gradlew :tax:batch:bootJar --console=plain --no-daemon; \
    jar_count="$(find tax/batch/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | wc -l | tr -d ' ')"; \
    test "$jar_count" -eq 1; \
    jar_file="$(find tax/batch/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print)"; \
    cp "$jar_file" /workspace/app.jar

FROM docker.io/library/eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app
COPY --from=builder --chown=app:app /workspace/app.jar /app/app.jar

USER app:app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
