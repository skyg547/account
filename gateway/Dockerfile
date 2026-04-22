# 뼈대: 자바 21
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# 빌드된 jar 파일 복사
COPY build/libs/*.jar app.jar

# Gateway 포트 노출
EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
