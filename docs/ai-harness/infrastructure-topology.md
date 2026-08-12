# MSA Infrastructure Topology Guide

본 문서는 `c:\dev\account` 저장소의 Podman / Docker Compose 기반 22개 마이크로서비스 구동 및 인프라 토폴로지를 설명합니다.

## 아키텍처 개요
- **단일 진실 공급원(SSOT)**: 빌드 환경(`build.gradle`의 `Java 17` 스펙)을 엄격히 준수합니다.
- **모듈별 독립 Dockerfile**: 마이크로서비스(MSA)의 독립적 배포와 관리를 보장하기 위해 각 모듈(API/Batch)은 자기 자신의 경로(`[module-path]/Dockerfile`)를 보유하고 있습니다.
- **멀티 스테이지 빌드**: 모든 개별 `Dockerfile`은 `gradle:8.7-jdk17-alpine` 이미지를 통해 소스를 컴파일하고, 런타임에는 `eclipse-temurin:17-jre-alpine`으로 경량화된 실행 환경을 구성합니다.

## 개발용 컨테이너 실행 (Local Dev)
개발자는 `docker-compose.yml`의 `profiles`를 사용하여 원하는 서비스 그룹만 선택적으로 띄울 수 있습니다.

### 편의 스크립트 활용
```bash
# 기본(platform, apis, self-contained) 서비스 띄우기
./start-dev.sh

# 특정 프로파일만 띄우기
./start-dev.sh "platform,batch"
```

## 상용 환경 배포 (Production Overlay)
상용 배포 시에는 로컬 볼륨 마운트나 내부 DB(PostgreSQL) 컨테이너 기동을 막아야 합니다. 이를 위해 오버레이 설정 파일을 사용합니다.

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```
