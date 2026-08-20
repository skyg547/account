# [초보자를 위한 멀티스테이지 빌드]
# 1단계는 루트 Gradle 멀티모듈에서 Config Server 실행 JAR을 만들고, 2단계는 JRE와 실행 JAR만 남깁니다.
FROM docker.io/library/gradle:8.7-jdk17-alpine AS builder
USER root
RUN mkdir -p /workspace && chown gradle:gradle /workspace
USER gradle
WORKDIR /workspace

# config-server도 루트 settings.gradle/build.gradle을 사용하므로 저장소 루트가 Docker build context여야 합니다.
COPY --chown=gradle:gradle . .
RUN set -eu; \
    ./gradlew :config-server:bootJar --console=plain --no-daemon; \
    jar_count="$(find config-server/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | wc -l | tr -d ' ')"; \
    test "$jar_count" -eq 1; \
    jar_file="$(find config-server/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print)"; \
    cp "$jar_file" /workspace/app.jar

FROM docker.io/library/eclipse-temurin:17-jre-alpine AS runtime
WORKDIR /app

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

RUN addgroup -S app && adduser -S -G app app
COPY --from=builder --chown=app:app /workspace/app.jar /app/app.jar
# 설정 원본은 이미지 레이어에 넣지 않습니다. 실행 시 승인된 read-only volume을 /config-repo에 연결합니다.

ENV CONFIG_REPO_LOCATION=file:/config-repo
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# 프로세스가 아니라 실제 설정 저장소까지 읽을 수 있을 때만 뒤쪽 서비스가 시작됩니다.
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
    CMD wget -q -O /dev/null http://localhost:8888/actuator/health/readiness || exit 1

USER app:app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
