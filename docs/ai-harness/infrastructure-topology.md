# Infrastructure Topology Guide

본 문서는 `account` 저장소의 Podman / Docker Compose 기반 마이크로서비스 인프라 토폴로지를 설명합니다. 
로컬 환경(dev) 및 상용 배포(prod) 시 컨테이너 오케스트레이션 구성에 대한 단일 진실 공급원(SSOT) 역할을 합니다.

## 단일 Containerfile 아키텍처
전체 22개 서비스는 루트 경로의 `Containerfile` 하나를 공유합니다. 
`docker-compose.yml`에서 `build.args`로 `GRADLE_PROJECT` 및 `JAR_DIRECTORY`를 동적으로 주입하여 개별 이미지를 빌드합니다.
하위 디렉터리에 흩어져 있던 `Dockerfile`들은 중복 빌드 방지 및 일관성을 위해 모두 제거되었습니다.

## Docker Compose Profiles

MSA 환경 특성상 모든 서비스를 항상 띄워둘 필요가 없을 때 리소스를 절약하기 위해 **Docker Compose Profiles**를 사용합니다.

- **`self-contained`**: DB(PostgreSQL), Redis, Kafka 등 필수 인프라. 외부 인프라가 없는 로컬 개발 시 필수.
- **`platform`**: API Gateway, Frontend, Config Server, Eureka Discovery 등 플랫폼 계층 서비스.
- **`apis`**: 모든 도메인 API 서비스 그룹.
- **`batch`**: 모든 도메인 Batch 서비스 그룹.
- **`risk` / `accounting` / `products` / `foundation`**: 도메인 바운디드 컨텍스트별 하위 프로파일.
- **`migration`**: Flyway DB 마이그레이션 실행 러너.

### 사용 예시 (로컬 개발)

```bash
# 전체 API 및 프론트엔드 환경 구동
COMPOSE_PROFILES=self-contained,platform,apis docker-compose up -d

# 인프라 리소스만 구동 (백엔드는 IDE에서 직접 실행 시)
COMPOSE_PROFILES=self-contained docker-compose up -d

# 특정 도메인(accounting) API만 구동
COMPOSE_PROFILES=self-contained,platform,accounting docker-compose up -d
```

### 편의 스크립트

루트 디렉터리에 있는 `start-dev.sh`를 실행하면 권장 개발 환경 프로파일이 자동으로 적용되어 실행됩니다.

## Production Overlay (docker-compose.prod.yml)

상용 배포 시에는 루트 `docker-compose.yml`을 베이스로 삼고, `docker-compose.prod.yml` 오버레이 파일을 추가하여 구동합니다.

```bash
docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d
```

**Prod 오버레이 적용 사항:**
1. 볼륨 마운트(소스 코드) 제거
2. `restart: always` 정책 부여
3. 인프라(DB, Redis, Kafka)는 AWS RDS / MSK 등 Managed Service 사용을 전제로 하여 프로파일에서 제외하거나 명시적으로 구성 해제. 외부 URL을 환경변수로 주입받습니다.
4. 포트 외부 바인딩 최소화 (Gateway 8000, Frontend 3000 만 개방)
