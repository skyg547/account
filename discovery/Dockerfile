# [초보자를 위한 멀티스테이지 빌드]
# 1단계는 루트 Gradle 멀티모듈에서 Eureka 실행 JAR을 만들고, 2단계는 JRE와 JAR만 남깁니다.
FROM docker.io/library/gradle:8.7-jdk17-alpine AS builder
USER root
RUN mkdir -p /workspace && chown gradle:gradle /workspace
USER gradle
WORKDIR /workspace

# discovery도 루트 settings.gradle/build.gradle을 사용하므로 저장소 루트가 Docker build context여야 합니다.
COPY --chown=gradle:gradle . .
RUN set -eu; \
    ./gradlew :discovery:bootJar --console=plain --no-daemon; \
    jar_count="$(find discovery/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' | wc -l | tr -d ' ')"; \
    test "$jar_count" -eq 1; \
    jar_file="$(find discovery/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' -print)"; \
    cp "$jar_file" /workspace/app.jar

FROM docker.io/library/eclipse-temurin:17-jre-alpine

WORKDIR /app

RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

RUN addgroup -S app && adduser -S -G app app
COPY --from=builder --chown=app:app /workspace/app.jar /app/app.jar
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

# 단순 프로세스 시작이 아니라 actuator readiness가 UP이어야 뒤쪽 서비스가 등록을 시작할 수 있습니다.
HEALTHCHECK --interval=10s --timeout=3s --start-period=30s --retries=6 \
    CMD wget -q -O /dev/null http://localhost:8761/actuator/health/readiness || exit 1

USER app:app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
