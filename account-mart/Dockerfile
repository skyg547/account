# Stage 1: Build - Gradle 빌드 환경 (Java 21 활용)
FROM docker.io/library/gradle:8.7-jdk21-alpine AS builder
WORKDIR /app

# 멀티 모듈 의존성 해결을 위해 필요한 설정 및 모듈 복사
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY shared-kernel shared-kernel
COPY account-mart account-mart

# 마트 API 모듈 빌드 (기본 실행 유닛)
RUN ./gradlew :account-mart:mart-api:bootJar -x test

# Stage 2: Runtime - 경량 실행 환경
FROM docker.io/library/eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드 결과물(JAR) 복사
COPY --from=builder /app/account-mart/mart-api/build/libs/*-SNAPSHOT.jar app.jar

# 환경 변수 및 포트 설정
# 8085: IFRS 9 대손충당금 입력 마트 API 서비스 포트
ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8085

# Actuator를 통한 서비스 건강 상태 체크
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- http://localhost:8085/actuator/health || exit 1

# 실행 명령어
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
