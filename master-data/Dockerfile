# [초보자를 위한 멀티스테이지 빌드]
# 1단계는 루트 Gradle 멀티모듈에서 Master Data 실행 JAR을 만들고, 2단계는 JRE와 JAR만 남깁니다.
FROM gradle:8.7-jdk17-alpine AS builder
WORKDIR /build

# contracts/shared-kernel과 루트 Gradle 설정이 필요하므로 저장소 루트가 Docker build context여야 합니다.
COPY . .
RUN ./gradlew :master-data:api:bootJar --no-daemon \
    && find master-data/api/build/libs -maxdepth 1 -type f -name '*.jar' ! -name '*-plain.jar' \
    -exec cp {} /build/master-data-app.jar \;

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 한국 업무일자와 로그 시각을 로컬 운영 기준과 맞춥니다.
RUN apk add --no-cache tzdata \
    && cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime \
    && echo "Asia/Seoul" > /etc/timezone

COPY --from=builder /build/master-data-app.jar app.jar

# 프로세스만 떠 있는 상태가 아니라 DB/JPA/Config 연결을 포함한 readiness가 UP인지 확인합니다.
HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=6 \
    CMD wget -q -O /dev/null http://localhost:8082/actuator/health/readiness || exit 1

EXPOSE 8082

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
