# Stage 1: Build
FROM docker.io/library/gradle:8.7-jdk21-alpine AS builder
WORKDIR /app

# 모든 모듈 복제
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

RUN ./gradlew :credit-risk-service:credit-batch:bootJar -x test

# Stage 2: Runtime
FROM docker.io/library/eclipse-temurin:21-jre-alpine
WORKDIR /app

COPY --from=builder /app/credit-risk-service/credit-batch/build/libs/*-SNAPSHOT.jar app.jar

ENV SPRING_PROFILES_ACTIVE=docker

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar app.jar"]
