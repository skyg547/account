# 📜 Config Server (중앙 설정 서버) - "MSA 레시피 본사"

## 1. 초보자를 위한 개념 설명
수십 개의 마이크로서비스가 각각 자기 폴더 안에 `application.yml`을 가지고 있다면, 데이터베이스 비밀번호가 하나 바뀔 때마다 모든 폴더를 뒤져서 파일을 고치고 서버를 껐다 켜야 합니다.

**Config Server**는 프랜차이즈 **본사**입니다. 각 서비스(지점)들은 켜질 때 이 본사(포트 8888)에 접속하여 "나 master-data인데, 내 설정 파일(레시피) 좀 줘!" 라고 요청해서 받아갑니다. 

## 2. 이 폴더의 역할
- `ConfigServerApplication`: 8888 포트로 켜져서 설정 파일(yml)을 서빙해 주는 웹 서버입니다.
- 실제 각 서비스들의 설정 파일 내용물은 **프로젝트 최상단의 `config-repo` 폴더**에 모아둡니다.

## 3. 실행 방법
전체 시스템 중 **가장 먼저** 켜져야 하는 서버입니다.
```bash
./gradlew :config-server:bootRun
```

## 4. Docker 로 실행하기
이 폴더에 있는 `Dockerfile`을 통해 도커 이미지로 구울 수 있습니다.
```bash
docker build -t account/config-server .
docker run -p 8888:8888 account/config-server
```
