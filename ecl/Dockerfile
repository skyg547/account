# Stage 1: Build - credit-api 실행 JAR 생성
FROM docker.io/library/gradle:8.7-jdk21-alpine AS builder
WORKDIR /app

COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY common common
COPY credit-risk-service credit-risk-service

RUN ./gradlew :credit-risk-service:credit-api:bootJar -x test

# Stage 2: Runtime - credit-api 실행 환경
FROM docker.io/library/eclipse-temurin:21-jre-alpine
LABEL maintainer="risk-team"

WORKDIR /app

# 루트 서비스 이미지는 대표 실행 유닛인 credit-api를 패키징합니다.
COPY --from=builder /app/credit-risk-service/credit-api/build/libs/*-SNAPSHOT.jar app.jar

# 리스크 API 서비스 포트 노출
ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8083

# 컨테이너의 건강 상태를 체크합니다. Actuator의 health 엔드포인트를 활용합니다.
HEALTHCHECK --interval=30s --timeout=3s \
  CMD wget -qO- http://localhost:8083/actuator/health || exit 1

# 컨테이너 실행 시 기본적으로 docker 프로필을 활성화하여 실행합니다.
# urandom 사용으로 보안 난수 생성 속도를 높여 부팅 속도를 개선합니다.
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
