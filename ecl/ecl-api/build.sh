#!/bin/bash
# Credit Risk API 빌드 스크립트
# Gradle을 사용하여 최신 소스코드를 컴파일하고 실행 가능한 JAR 파일을 생성합니다.

echo "[Build] Credit Risk API 빌드를 시작합니다..."
cd ../..
./gradlew :credit-risk-service:credit-api:clean :credit-risk-service:credit-api:bootJar
echo "[Build] Credit Risk API 빌드가 완료되었습니다."
