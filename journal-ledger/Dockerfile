# 뼈대: 자바 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 빌드된 jar 파일 복사 (멀티 모듈 구조이므로 api 하위의 jar를 바라봅니다)
COPY api/build/libs/*.jar app.jar

# Journal Ledger 포트 노출
EXPOSE 8081

ENTRYPOINT ["java", "-jar", "app.jar"]
