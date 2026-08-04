FROM docker.io/library/gradle:8.7-jdk17-alpine AS builder
USER root
RUN mkdir -p /workspace && chown gradle:gradle /workspace
USER gradle
WORKDIR /workspace

ARG GRADLE_PROJECT
ARG JAR_DIRECTORY

COPY --chown=gradle:gradle . .

RUN set -eu; \
    printf '%s' "$GRADLE_PROJECT" | grep -Eq '^:[a-z0-9-]+(:[a-z0-9-]+)*$'; \
    printf '%s' "$JAR_DIRECTORY" | grep -Eq '^[a-z0-9-]+(/[a-z0-9-]+)*$'; \
    test -f "$JAR_DIRECTORY/build.gradle"; \
    ./gradlew "${GRADLE_PROJECT}:bootJar" --console=plain --no-daemon; \
    jar_count="$(find "$JAR_DIRECTORY/build/libs" -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | wc -l | tr -d ' ')"; \
    test "$jar_count" -eq 1; \
    jar_file="$(find "$JAR_DIRECTORY/build/libs" -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print)"; \
    cp "$jar_file" /workspace/app.jar

FROM docker.io/library/eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN addgroup -S app && adduser -S -G app app

COPY --from=builder --chown=app:app /workspace/app.jar /app/app.jar

USER app:app
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
