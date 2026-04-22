# 🗺️ Discovery Service (유레카 서버) - "사내 전화번호부"

## 1. 초보자를 위한 개념 설명
MSA 환경에서는 서버(지점)가 수십 개로 늘어나고, 트래픽에 따라 IP 주소와 포트가 수시로 바뀝니다. 
**Discovery Service (Eureka)**는 모든 마이크로서비스가 켜질 때마다 "저 지금 8082번 포트에서 영업 시작했습니다!" 하고 신고(Register)하는 **'사내 중앙 전화번호부'**입니다.
다른 서버가 `master-data`를 찾고 싶을 때, IP 주소 대신 `master-data`라는 이름만 대면 유레카가 알아서 현재 영업 중인 IP 주소를 알려줍니다.

## 2. 실행 방법
MSA 시스템을 기동할 때 **Config Server 다음으로 가장 먼저** 켜져야 하는 핵심 인프라입니다.
```bash
./gradlew :discovery:bootRun
```
브라우저에서 `http://localhost:8761`에 접속하면, 현재 우리 시스템에 어떤 마이크로서비스들이 살아서 숨 쉬고 있는지(Instances currently registered) 한눈에 볼 수 있는 대시보드가 열립니다!

## 3. Docker 로 실행하기
이 폴더에 있는 `Dockerfile`을 통해 이 서비스를 레고 블록(도커 이미지)으로 만들 수 있습니다.
```bash
docker build -t account/discovery-service .
docker run -p 8761:8761 account/discovery-service
```
