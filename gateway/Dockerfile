# [초보자를 위한 멀티스테이지 빌드]
# 1단계는 Gradle로 실행 JAR을 만들고, 2단계는 JRE와 JAR만 남겨 이미지 크기와 공격 표면을 줄입니다.
FROM gradle:8.7-jdk17-alpine AS builder
WORKDIR /build

# Gateway도 루트 settings.gradle/build.gradle을 사용하는 멀티모듈이므로 저장소 루트가 빌드 컨텍스트여야 합니다.
COPY . .
RUN ./gradlew :gateway:bootJar --no-daemon && \
    find gateway/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' \
    -exec cp {} /build/gateway-app.jar \;

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 로그와 회계 업무 일자의 기본 표시 시간을 한국 시간으로 맞춥니다.
RUN apk add --no-cache tzdata && \
    cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime && \
    echo "Asia/Seoul" > /etc/timezone

COPY --from=builder /build/gateway-app.jar app.jar
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
