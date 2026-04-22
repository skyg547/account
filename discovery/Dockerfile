# 뼈대(OS 및 Java) 설정: 가볍고 빠른 알파인 리눅스 기반의 자바 21 버전 사용
FROM eclipse-temurin:21-jre-alpine

# 레고 블록의 이름표(작업 공간) 설정
WORKDIR /app

# 빌드된 스프링 부트 파일(jar)을 레고 블록 안으로 복사
# (이 Dockerfile을 실행하기 전에 ./gradlew :discovery:bootJar 명령어로 jar 파일을 먼저 만들어야 합니다)
COPY build/libs/*.jar app.jar

# 이 레고 블록은 8761번 포트를 씁니다!
EXPOSE 8761

# 도커 컨테이너가 켜질 때 실행할 명령어: "자바 프로그램을 실행해라!"
ENTRYPOINT ["java", "-jar", "app.jar"]
