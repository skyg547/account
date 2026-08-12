# [엔터프라이즈급 멀티스테이지 빌드]
# 1단계: 빌드 환경 (Builder)
# 소스코드를 가져와서 컴파일하고 실행 파일(.jar)을 만드는 과정입니다.
FROM gradle:8.7-jdk17-alpine AS builder
WORKDIR /build

# 전체 프로젝트 소스를 복사합니다. (멀티 모듈 의존성 해결을 위함)
COPY . .

# 해당 모듈만 선택하여 빌드합니다. (테스트는 생략하여 빌드 속도 향상)
# 엔터프라이즈 환경에서는 CI/CD 파이프라인에서 이미 테스트를 거치므로 여기선 패스합니다.
RUN ./gradlew :payable:build -x test --no-daemon

# 2단계: 실행 환경 (Runner)
# 무거운 빌드 도구(Gradle 등)를 버리고, 자바 실행기(JRE)만 있는 가벼운 이미지로 갈아탑니다.
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 타임존 설정 (한국 시간)
RUN apk add --no-cache tzdata &&     cp /usr/share/zoneinfo/Asia/Seoul /etc/localtime &&     echo "Asia/Seoul" > /etc/timezone

# 빌더 환경에서 생성된 .jar 파일만 쏙 빼옵니다. (이미지 용량 최적화)
COPY --from=builder /build/payable/build/libs/*-SNAPSHOT.jar app.jar
# 하위 모듈이 있을 경우 (예: journal-ledger/core)
COPY --from=builder /build/payable/*/build/libs/*-SNAPSHOT.jar app.jar || true
COPY --from=builder /build/payable/api/build/libs/*-SNAPSHOT.jar app.jar || true

# 실행 시 메모리 옵션 및 환경변수를 받을 수 있도록 설정합니다.
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
