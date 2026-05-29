# Stage 1: Build
FROM docker.io/library/gradle:8.7-jdk21-alpine AS builder
WORKDIR /app

# 대손충당금 배치와 공통 모듈 복제
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY shared-kernel shared-kernel
COPY ecl ecl

RUN ./gradlew :ecl:ecl-batch:bootJar -x test

# Stage 2: Runtime
FROM docker.io/library/eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/ecl/ecl-batch/build/libs/*-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
