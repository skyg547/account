#!/bin/bash
# IFRS 9 Allowance Batch 빌드 스크립트
# Gradle을 사용하여 최신 소스코드를 컴파일하고 실행 가능한 JAR 파일을 생성합니다.

echo "[Build] IFRS 9 Allowance Batch 빌드를 시작합니다..."
cd ../..
./gradlew :ecl:ecl-batch:clean :ecl:ecl-batch:bootJar
echo "[Build] IFRS 9 Allowance Batch 빌드가 완료되었습니다."
