# Stage 1: Build - ETL 배치 모듈 빌드
FROM docker.io/library/gradle:8.7-jdk21-alpine AS builder
WORKDIR /app

# 모든 연관 모듈 복제 (ETL은 전사 도메인 지식이 필요할 수 있음)
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY common common
COPY discovery-service discovery-service
COPY gateway-service gateway-service
COPY risk-data-mart-service risk-data-mart-service
COPY interest-rate-risk-service interest-rate-risk-service
COPY credit-risk-service credit-risk-service

# 마트 배치 모듈 빌드
RUN ./gradlew :risk-data-mart-service:mart-batch:bootJar -x test

# Stage 2: Runtime - 고성능 배치 실행 환경
FROM docker.io/library/eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드된 JAR 파일 복사
COPY --from=builder /app/risk-data-mart-service/mart-batch/build/libs/*-SNAPSHOT.jar app.jar

# 컨테이너 환경 변수 설정
ENV SPRING_PROFILES_ACTIVE=docker

# 실행 명령어: JVM 난수 생성기 최적화로 부팅 속도 개선
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
