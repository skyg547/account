# [초보자를 위한 멀티스테이지 빌드]
# 1단계는 Gradle로 실행 JAR을 만들고, 2단계는 JRE와 JAR만 남겨 이미지 크기와 공격 표면을 줄입니다.
FROM docker.io/library/gradle:8.7-jdk17-alpine AS builder
USER root
RUN mkdir -p /workspace && chown gradle:gradle /workspace
USER gradle
WORKDIR /workspace

# Gateway도 루트 settings.gradle/build.gradle을 사용하는 멀티모듈이므로 저장소 루트가 빌드 컨텍스트여야 합니다.
COPY --chown=gradle:gradle . .
RUN set -eu; \
    ./gradlew :gateway:bootJar --console=plain --no-daemon; \
    jar_count="$(find gateway/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | wc -l | tr -d ' ')"; \
    test "$jar_count" -eq 1; \
    jar_file="$(find gateway/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print)"; \
    cp "$jar_file" /workspace/app.jar

FROM docker.io/library/eclipse-temurin:17-jre-alpine

WORKDIR /app

# 로그와 회계 업무 일자의 기본 표시 시간을 한국 시간으로 맞춥니다.
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

RUN addgroup -S app && adduser -S -G app app
COPY --from=builder --chown=app:app /workspace/app.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"
USER app:app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
