# Stage 1: Build - Gradle 빌드 환경 구성
FROM docker.io/library/gradle:8.7-jdk21-alpine AS builder
WORKDIR /app

# 루트 및 의존성 프로젝트 복제
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY shared-kernel shared-kernel
COPY ecl ecl

# 대손충당금 API 모듈만 타겟팅하여 실행 가능한 JAR 생성
RUN ./gradlew :ecl:ecl-api:bootJar -x test

# Stage 2: Runtime - 경량화된 실행 환경
FROM docker.io/library/eclipse-temurin:21-jre-alpine
WORKDIR /app

# 빌드 단계에서 생성된 최신 JAR 파일만 복사
COPY --from=builder /app/ecl/ecl-api/build/libs/*-SNAPSHOT.jar app.jar

# 컨테이너 환경 변수 및 포트 설정
# 8083: IFRS 9 대손충당금 API 서비스 기본 포트
ENV SPRING_PROFILES_ACTIVE=docker
EXPOSE 8083

# 헬스체크: 유레카 서버 및 게이트웨이 연동 전 서비스 정상 구동 여부 확인
HEALTHCHECK --interval=20s --timeout=5s --retries=5 \
  CMD wget -qO- http://localhost:8083/actuator/health || exit 1

# 실행 명령어: JVM 최적화 옵션 포함
ENTRYPOINT ["java", "-Djava.security.egd=file:/dev/./urandom", "-Dspring.profiles.active=docker", "-jar", "app.jar"]
